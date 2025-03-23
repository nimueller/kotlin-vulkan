package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.toPointerList
import dev.cryptospace.anvil.vulkan.Vulkan
import dev.cryptospace.anvil.vulkan.VulkanSurface
import dev.cryptospace.anvil.vulkan.device.suitable.PhysicalDeviceSuitableCriteria
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties
import java.nio.ByteBuffer

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

    val extensions =
        MemoryStack.stackPush().use { stack ->
            val extensionCount = stack.mallocInt(1)
            val layerName: ByteBuffer? = null
            vkEnumerateDeviceExtensionProperties(handle, layerName, extensionCount, null)

            val availableExtensions = VkExtensionProperties.malloc(extensionCount[0], stack)
            vkEnumerateDeviceExtensionProperties(handle, layerName, extensionCount, availableExtensions)

            availableExtensions.map { DeviceExtension(it.extensionNameString()) }
        }

    val name: String = properties.deviceNameString()

    var presentQueueFamilyIndex = -1
        private set

    fun refreshPresentQueueFamilyIndex(surface: VulkanSurface) {
        check(isAlive) { "device is already destroyed" }
        check(surface.isAlive) { "surface is already destroyed" }

        presentQueueFamilyIndex = -1

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
                    presentQueueFamilyIndex = index
                    return
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
            return PhysicalDeviceSuitableCriteria.allCriteriaSatisfied(device)
        }
    }
}
