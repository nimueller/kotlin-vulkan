package dev.cryptospace.vulkan.core.device

import dev.cryptospace.anvil.core.toPointerList
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties

data class PhysicalDevice(val handle: VkPhysicalDevice) : AutoCloseable {

    val properties =
        VkPhysicalDeviceProperties.malloc().apply {
            vkGetPhysicalDeviceProperties(handle, this)
        }
    val features =
        VkPhysicalDeviceFeatures.malloc().apply {
            vkGetPhysicalDeviceFeatures(handle, this)
        }
    val queueFamilies =
        MemoryStack.stackPush().use { stack ->
            val countBuffer = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, null)

            val queueBuffer = VkQueueFamilyProperties.malloc(countBuffer[0])
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, queueBuffer)

            queueBuffer
        }
    val graphicsQueueFamilyIndex =
        queueFamilies.indexOfFirst { it.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0 }

    val name: String = properties.deviceNameString()

    override fun toString(): String {
        return name
    }

    override fun close() {
        properties.free()
        features.free()
        queueFamilies.free()
    }

    companion object {

        fun List<PhysicalDevice>.pickBestDevice(): PhysicalDevice {
            val selectedDevice = firstOrNull { isPhysicalDeviceSuitable(it) }
            checkNotNull(selectedDevice) { "Failed to find a suitable GPU" }
            return selectedDevice
        }

        fun listPhysicalDevices(vulkanInstance: VkInstance) = MemoryStack.stackPush().use { stack ->
            val deviceCountBuffer = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(vulkanInstance, deviceCountBuffer, null)
            check(deviceCountBuffer[0] != 0) { "Failed to find GPUs with Vulkan support" }

            val devicesBuffer = stack.mallocPointer(1)
            vkEnumeratePhysicalDevices(vulkanInstance, deviceCountBuffer, devicesBuffer)

            return@use devicesBuffer.toPointerList()
                .map { VkPhysicalDevice(it, vulkanInstance) }
                .map { PhysicalDevice(it) }
        }

        private fun isPhysicalDeviceSuitable(device: PhysicalDevice): Boolean {
            val supportsGraphics = device.graphicsQueueFamilyIndex >= 0
            return supportsGraphics
        }
    }
}
