package dev.cryptospace.vulkan.core.device

import dev.cryptospace.vulkan.utils.getLogger
import dev.cryptospace.vulkan.utils.toList
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice

object VulkanPhysicalDeviceSelector {
    @JvmStatic
    private val logger = getLogger<VulkanPhysicalDeviceSelector>()

    fun pickDevice(vulkanInstance: VkInstance): PhysicalDevice {
        val devices = listPhysicalDevices(vulkanInstance)
        logger.info("Found physical devices: $devices")
        val selectedDevice = devices.firstOrNull { isPhysicalDeviceSuitable(it) }
        checkNotNull(selectedDevice) { "Failed to find a suitable GPU" }
        logger.info("Selected suitable physical device: $selectedDevice")
        return selectedDevice
    }

    private fun listPhysicalDevices(vulkanInstance: VkInstance) = MemoryStack.stackPush().use { stack ->
        val deviceCountBuffer = stack.mallocInt(1)
        vkEnumeratePhysicalDevices(vulkanInstance, deviceCountBuffer, null)
        check(deviceCountBuffer[0] != 0) { "Failed to find GPUs with Vulkan support" }

        val devicesBuffer = stack.mallocPointer(1)
        vkEnumeratePhysicalDevices(vulkanInstance, deviceCountBuffer, devicesBuffer)

        return@use devicesBuffer.toList()
            .map { VkPhysicalDevice(it, vulkanInstance) }
            .map { PhysicalDevice(it) }
    }

    private fun isPhysicalDeviceSuitable(device: PhysicalDevice): Boolean {
        val supportsGraphics = device.graphicsQueueFamily != null
        return supportsGraphics
    }
}
