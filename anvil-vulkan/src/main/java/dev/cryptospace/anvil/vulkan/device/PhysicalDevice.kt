package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.toPointerList
import dev.cryptospace.anvil.vulkan.Vulkan
import dev.cryptospace.anvil.vulkan.VulkanSurface
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties

data class PhysicalDevice(val vulkan: Vulkan, val handle: VkPhysicalDevice) : NativeResource() {

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

    fun getPresentationQueueFamilyIndex(surface: VulkanSurface): Int {
        check(isAlive) { "device is already destroyed" }
        check(surface.isAlive) { "surface is already destroyed" }

        MemoryStack.stackPush().use { stack ->
            val presentSupport = stack.mallocInt(1)

            queueFamilies.forEachIndexed { index, _ ->
                val result = vkGetPhysicalDeviceSurfaceSupportKHR(
                    handle,
                    index,
                    surface.handle,
                    presentSupport
                )
                check(result == VK_SUCCESS) { "Failed to query for surface support capabilities" }

                if (presentSupport[0] != VK_FALSE) {
                    return index
                }
            }
        }

        error("Failed to find a suitable queue family which supported presentation queue")
    }

    override fun toString(): String {
        return "${this::class.simpleName}(name=$name)"
    }

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
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

        fun listPhysicalDevices(vulkan: Vulkan) = MemoryStack.stackPush().use { stack ->
            val instance = vulkan.instance

            val deviceCountBuffer = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(instance, deviceCountBuffer, null)
            check(deviceCountBuffer[0] != 0) { "Failed to find GPUs with Vulkan support" }

            val devicesBuffer = stack.mallocPointer(1)
            vkEnumeratePhysicalDevices(instance, deviceCountBuffer, devicesBuffer)

            return@use devicesBuffer.toPointerList()
                .map { VkPhysicalDevice(it, instance) }
                .map { PhysicalDevice(vulkan, it) }
        }

        private fun isPhysicalDeviceSuitable(device: PhysicalDevice): Boolean {
            val supportsGraphics = device.graphicsQueueFamilyIndex >= 0
            return supportsGraphics
        }
    }
}
