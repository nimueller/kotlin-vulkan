package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.toList
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.getVulkanBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

object SurfaceSwapChainDetailsFactory {
    fun create(physicalDevice: PhysicalDevice, surface: Surface): SurfaceSwapChainDetails {
        physicalDevice.validateNotDestroyed()
        surface.validateNotDestroyed()

        val surfaceCapabilities = getCapabilities(surface, physicalDevice)
        val surfaceFormats = getFormats(surface, physicalDevice)
        val presentModes = getPresentModes(surface, physicalDevice)

        return SurfaceSwapChainDetails(
            physicalDevice = physicalDevice,
            surfaceCapabilities = surfaceCapabilities,
            formats = surfaceFormats,
            presentModes = presentModes,
        )
    }

    private fun getCapabilities(surface: Surface, physicalDevice: PhysicalDevice): SurfaceCapabilities {
        MemoryStack.stackPush().use { stack ->
            val capabilities = VkSurfaceCapabilitiesKHR.malloc(stack)
            val result =
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                    physicalDevice.handle,
                    surface.address.handle,
                    capabilities,
                )
            check(result == VK_SUCCESS) { "Failed to get surface capabilities" }
            return SurfaceCapabilities(
                device = physicalDevice,
                surface = surface,
            )
        }
    }

    private fun getFormats(surface: Surface, physicalDevice: PhysicalDevice): List<SurfaceFormat> {
        val formats =
            getVulkanBuffer(
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

        return formats.map {
            SurfaceFormat(
                device = physicalDevice,
                surface = surface,
            )
        }
    }

    private fun getPresentModes(surface: Surface, physicalDevice: PhysicalDevice): List<SurfacePresentMode> {
        val presentModes =
            getVulkanBuffer(
                bufferQuery = { countBuffer, resultBuffer ->
                    vkGetPhysicalDeviceSurfacePresentModesKHR(
                        physicalDevice.handle,
                        surface.address.handle,
                        countBuffer,
                        resultBuffer,
                    )
                },
            )

        if (presentModes == null) {
            return emptyList()
        }

        return presentModes.toList().map {
            SurfacePresentMode(
                device = physicalDevice,
                surface = surface,
            )
        }
    }
}
