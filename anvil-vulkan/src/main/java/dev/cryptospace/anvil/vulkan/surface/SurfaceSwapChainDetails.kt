package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.warn
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.queryVulkanIntBuffer
import dev.cryptospace.anvil.vulkan.transform
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

data class SurfaceSwapChainDetails(
    private val physicalDevice: PhysicalDevice,
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
                physicalDevice.handle,
                surface.address.handle,
                capabilities,
            ).validateVulkanSuccess()
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
                        physicalDevice.handle,
                        surface.address.handle,
                        countBuffer,
                        resultBuffer,
                    )
                },
            )
        }

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
                    physicalDevice.handle,
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

    override fun destroy() {
        surfaceCapabilities.free()
        surfaceFormats.free()
    }

    companion object {

        @JvmStatic
        private val logger = logger<SurfaceSwapChainDetails>()
    }
}
