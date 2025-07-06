package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.warn
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.getFramebufferSize
import dev.cryptospace.anvil.vulkan.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.queryVulkanIntBuffer
import dev.cryptospace.anvil.vulkan.transform
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR
import org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

data class SurfaceSwapChainDetails(
    private val physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
    private val surface: Surface,
) : NativeResource() {

    /**
     * Contains the surface capabilities for the physical device and surface combination.
     * This includes min/max image count, min/max image extent, supported transforms, and other
     * surface-specific capabilities.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val surfaceCapabilities: VkSurfaceCapabilitiesKHR =
        VkSurfaceCapabilitiesKHR.malloc().also { capabilities ->
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                physicalDeviceSurfaceInfo.physicalDevice.handle,
                surface.address.handle,
                capabilities,
            ).validateVulkanSuccess()
        }

    /**
     * Represents the dimensions (width and height) of the swap chain images.
     * If the current extent is set to -1 (which corresponds to the maximum uint32_t value in Vulkan),
     * it indicates that the window manager allows the swap chain extent to differ from the window size.
     * In this case, we calculate the extent based on the window's framebuffer size,
     * clamped between the minimum and maximum supported extents.
     * Otherwise, uses the surface's current extent directly.
     *
     * The swap extent is crucial for determining the size of the swap chain images and
     * must match the window's dimensions for proper rendering. The special -1 value check
     * allows for flexible window resizing while maintaining proper rendering dimensions within
     * the supported bounds of the surface capabilities.
     */
    val swapExtent: VkExtent2D = VkExtent2D.malloc().also { extent ->
        if (surfaceCapabilities.currentExtent().width() != -1) {
            extent.set(surfaceCapabilities.currentExtent())
        } else {
            val framebufferSize = surface.window.getFramebufferSize()
            val width = Math.clamp(
                framebufferSize.width.toLong(),
                surfaceCapabilities.minImageExtent().width(),
                surfaceCapabilities.maxImageExtent().width(),
            )
            val height = Math.clamp(
                framebufferSize.height.toLong(),
                surfaceCapabilities.minImageExtent().height(),
                surfaceCapabilities.maxImageExtent().height(),
            )

            extent.width(width).height(height)
        }
    }

    /**
     * Contains a buffer of available surface formats for the physical device and surface combination.
     * Each surface format specifies the color space and pixel format that can be used for the surface.
     * This buffer is automatically allocated and must be freed when no longer needed.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val surfaceFormats: VkSurfaceFormatKHR.Buffer =
        MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBuffer(
                bufferInitializer = { VkSurfaceFormatKHR.malloc(it) },
                bufferQuery = { countBuffer, resultBuffer ->
                    vkGetPhysicalDeviceSurfaceFormatsKHR(
                        physicalDeviceSurfaceInfo.physicalDevice.handle,
                        surface.address.handle,
                        countBuffer,
                        resultBuffer,
                    )
                },
            )
        }

    /**
     * Selects the most suitable surface format for the swap chain.
     * Prioritizes SRGB color space with B8G8R8A8 format for optimal color representation.
     * If the preferred format is not available, it falls back to the first available format.
     *
     * @return The selected VkSurfaceFormatKHR that will be used for the swap chain
     */
    val bestSurfaceFormat: VkSurfaceFormatKHR =
        surfaceFormats.firstOrNull { surfaceFormat ->
            surfaceFormat.format() == VK_FORMAT_B8G8R8A8_SRGB &&
                surfaceFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
        } ?: surfaceFormats.first()

    /**
     * Contains a list of supported presentation modes for the physical device and surface combination.
     * Each present mode represents a different method for displaying rendered images to the surface,
     * such as immediate, FIFO (vertical sync), or relaxed FIFO.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val surfacePresentModes: List<SurfacePresentMode> =
        MemoryStack.stackPush().use { stack ->
            stack.queryVulkanIntBuffer { countBuffer, resultBuffer ->
                vkGetPhysicalDeviceSurfacePresentModesKHR(
                    physicalDeviceSurfaceInfo.physicalDevice.handle,
                    surface.address.handle,
                    countBuffer,
                    resultBuffer,
                )
            }.transform { value ->
                SurfacePresentMode.fromVulkanValue(value).also { presentMode ->
                    if (presentMode == null) {
                        logger.warn { "Unknown surface present mode: $value" }
                    }
                }
            }
        }

    /**
     * Selects the most suitable presentation mode for the swap chain.
     * Attempts to use the MAILBOX mode first for triple buffering with minimal latency.
     * Falls back to FIFO mode (vertical sync) if MAILBOX is not available, which is
     * guaranteed to be supported on all devices.
     *
     * @return The selected presentation mode that will be used for the swap chain
     */
    val bestPresentMode: SurfacePresentMode =
        surfacePresentModes.firstOrNull { surfacePresentMode ->
            surfacePresentMode == SurfacePresentMode.MAILBOX
        } ?: SurfacePresentMode.FIFO

    override fun destroy() {
        surfaceCapabilities.free()
        surfaceFormats.free()
    }

    companion object {

        @JvmStatic
        private val logger = logger<SurfaceSwapChainDetails>()
    }
}
