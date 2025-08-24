package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.warn
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.utils.getFramebufferSize
import dev.cryptospace.anvil.vulkan.utils.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.utils.queryVulkanIntBuffer
import dev.cryptospace.anvil.vulkan.utils.transform
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR
import org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

/**
 * Encapsulates all the necessary details required for creating and managing a Vulkan swap chain.
 * This includes surface capabilities, optimal extent, supported formats, and presentation modes.
 *
 * The class queries and stores various surface-related information from the physical device,
 * which is essential for creating a properly configured swap chain that can efficiently
 * present rendered images to the screen.
 *
 * Properties in this class help determine:
 * - Image count and dimensions for the swap chain
 * - Color format and space for rendering
 * - Presentation mode for displaying frames
 * - Surface transformation capabilities
 *
 * This class implements [NativeResource] to properly manage Vulkan resources that need
 * to be explicitly freed when no longer needed.
 */
data class SurfaceSwapChainDetails(
    /**
     * Contains the surface capabilities for the physical device and surface combination.
     * This includes min/max image count, min/max image extent, supported transforms, and other
     * surface-specific capabilities.
     */
    val surfaceCapabilities: VkSurfaceCapabilitiesKHR,

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
    val swapChainExtent: VkExtent2D,

    /**
     * Contains a buffer of available surface formats for the physical device and surface combination.
     * Each surface format specifies the color space and pixel format that can be used for the surface.
     * This buffer is automatically allocated and must be freed when no longer needed.
     */
    val surfaceFormats: VkSurfaceFormatKHR.Buffer,

    /**
     * Contains a list of supported presentation modes for the physical device and surface combination.
     * Each present mode represents a different method for displaying rendered images to the surface,
     * such as immediate, FIFO (vertical sync), or relaxed FIFO.
     */
    val surfacePresentModes: List<SurfacePresentMode>,
) : NativeResource() {

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

        /**
         * Queries and collects all necessary surface and swap chain related details from the physical device.
         * This includes surface capabilities, formats, presentation modes, and optimal swap chain extent.
         *
         * The method performs several Vulkan API calls to gather information:
         * - Surface capabilities (min/max image count, extents, etc.)
         * - Supported surface formats (color format and space combinations)
         * - Available presentation modes
         * - Optimal swap chain extent based on window size and surface capabilities
         *
         * @param physicalDeviceSurfaceInfo Information about the physical device and its surface support
         * @return A [SurfaceSwapChainDetails] object containing all queried information needed for swap chain creation
         */
        fun query(physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo): SurfaceSwapChainDetails =
            MemoryStack.stackPush().use { stack ->
                val surface = physicalDeviceSurfaceInfo.surface

                val surfaceCapabilities = querySurfaceCapabilities(physicalDeviceSurfaceInfo, surface)
                val swapChainExtent = querySwapChainExtent(surfaceCapabilities, surface)
                val surfaceFormats = querySurfaceFormats(stack, physicalDeviceSurfaceInfo, surface)
                val presentModes = queryPresentModes(stack, physicalDeviceSurfaceInfo, surface)

                return SurfaceSwapChainDetails(
                    surfaceCapabilities,
                    swapChainExtent,
                    surfaceFormats,
                    presentModes,
                ).also { swapChainDetails ->
                    logger.info("Surface swap chain details: $swapChainDetails")
                }
            }

        /**
         * Queries the surface capabilities for the given physical device and surface combination.
         *
         * @param physicalDeviceSurfaceInfo Information about the physical device
         * @param surface The surface to query capabilities for
         * @return VkSurfaceCapabilitiesKHR containing the queried capabilities
         */
        private fun querySurfaceCapabilities(
            physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
            surface: Surface,
        ): VkSurfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.malloc().also { capabilities ->
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                physicalDeviceSurfaceInfo.physicalDevice.handle,
                surface.handle.value,
                capabilities,
            ).validateVulkanSuccess()
        }

        /**
         * Determines the optimal swap chain extent (dimensions) based on surface capabilities and window size.
         *
         * The method handles two scenarios:
         * 1. If the current extent width is not -1, uses the surface's current extent directly
         * 2. Otherwise, calculates the extent based on the window's framebuffer size, clamped to
         *    the surface's minimum and maximum supported dimensions
         *
         * @param surfaceCapabilities The surface capabilities containing extent constraints
         * @param surface The surface associated with the window
         * @return VkExtent2D containing the calculated swap chain dimensions
         */
        private fun querySwapChainExtent(surfaceCapabilities: VkSurfaceCapabilitiesKHR, surface: Surface): VkExtent2D =
            VkExtent2D.malloc().also { extent ->
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
         * Queries all supported surface formats for the given physical device and surface combination.
         *
         * @param stack The memory stack used for temporary allocations
         * @param physicalDeviceSurfaceInfo Information about the physical device
         * @param surface The surface to query formats for
         * @return Buffer containing all supported surface formats
         */
        private fun querySurfaceFormats(
            stack: MemoryStack,
            physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
            surface: Surface,
        ): VkSurfaceFormatKHR.Buffer = stack.queryVulkanBuffer(
            bufferInitializer = { size -> VkSurfaceFormatKHR.malloc(size) },
            bufferQuery = { countBuffer, resultBuffer ->
                vkGetPhysicalDeviceSurfaceFormatsKHR(
                    physicalDeviceSurfaceInfo.physicalDevice.handle,
                    surface.handle.value,
                    countBuffer,
                    resultBuffer,
                )
            },
        )

        /**
         * Queries all supported presentation modes for the given physical device and surface combination.
         * Transforms the raw Vulkan values into [SurfacePresentMode] enum values.
         *
         * @param stack The memory stack used for temporary allocations
         * @param physicalDeviceSurfaceInfo Information about the physical device
         * @param surface The surface to query present modes for
         * @return List of supported presentation modes
         */
        private fun queryPresentModes(
            stack: MemoryStack,
            physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
            surface: Surface,
        ): List<SurfacePresentMode> = stack.queryVulkanIntBuffer { countBuffer, resultBuffer ->
            vkGetPhysicalDeviceSurfacePresentModesKHR(
                physicalDeviceSurfaceInfo.physicalDevice.handle,
                surface.handle.value,
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
}
