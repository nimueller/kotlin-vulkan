package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue

/**
 * Represents a logical Vulkan device that provides the main interface for interacting with a physical GPU.
 * Manages device-specific resources, queues and provides functionality for creating swap chains.
 *
 * @property handle The native Vulkan device handle
 * @property physicalDeviceSurfaceInfo Information about the physical device and its surface capabilities
 */
data class LogicalDevice(
    val handle: VkDevice,
    val physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
) : NativeResource() {

    /** The physical device (GPU) associated with this logical device */
    val physicalDevice: PhysicalDevice = physicalDeviceSurfaceInfo.physicalDevice

    /** Queue used for submitting graphics commands to the GPU */
    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDeviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    /** Queue used for presenting rendered images to the surface */
    val presentQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDeviceSurfaceInfo.presentQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    override fun destroy() {
        vkDestroyDevice(handle, null)
    }
}
