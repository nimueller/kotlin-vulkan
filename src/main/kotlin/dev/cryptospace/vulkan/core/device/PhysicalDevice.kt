package dev.cryptospace.vulkan.core.device

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties

data class PhysicalDevice(val handle: VkPhysicalDevice) {

    val properties by lazy {
        VkPhysicalDeviceProperties.malloc().apply {
            vkGetPhysicalDeviceProperties(handle, this)
        }
    }
    val features by lazy {
        VkPhysicalDeviceFeatures.malloc().apply {
            vkGetPhysicalDeviceFeatures(handle, this)
        }
    }
    val queueFamilies by lazy {
        MemoryStack.stackPush().use { stack ->
            val countBuffer = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, null)

            val queueBuffer = VkQueueFamilyProperties.malloc(countBuffer[0])
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, queueBuffer)

            queueBuffer
        }
    }
    val graphicsQueueFamily by lazy {
        queueFamilies.firstOrNull { it.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0 }
    }

    val name: String = properties.deviceNameString()

    override fun toString(): String {
        return name
    }
}
