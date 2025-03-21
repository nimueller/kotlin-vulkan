package dev.cryptospace.vulkan.core.device

import dev.cryptospace.anvil.core.logger
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateDevice
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkQueue

data class LogicalDevice(val handle: VkDevice, val physicalDevice: PhysicalDevice) : AutoCloseable {

    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    override fun close() {
        vkDestroyDevice(handle, null)
    }

    companion object {

        @JvmStatic
        private val logger = logger<LogicalDevice>()

        fun create(device: PhysicalDevice): LogicalDevice = MemoryStack.stackPush().use { stack ->
            val graphicsQueueFamily = device.graphicsQueueFamilyIndex
            checkNotNull(graphicsQueueFamily) { "Got an invalid graphics queue family index" }

            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsQueueFamily)
                .pQueuePriorities(stack.floats(0.0f))
                .flags(0)

            val features = VkPhysicalDeviceFeatures.calloc(stack)

            val queueCreateInfoBuffer = VkDeviceQueueCreateInfo.malloc(1, stack)
            queueCreateInfoBuffer.put(queueCreateInfo)
            queueCreateInfoBuffer.flip()

            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfoBuffer)
                .ppEnabledLayerNames(stack.mallocPointer(0))
                .ppEnabledExtensionNames(stack.mallocPointer(0))
                .pEnabledFeatures(features)

            val deviceBuffer = stack.mallocPointer(1)
            val result = vkCreateDevice(device.handle, deviceCreateInfo, null, deviceBuffer)
            check(result == VK_SUCCESS) { "Failed to create logical device: $result" }
            logger.info("Created logical device for ${device.name}")

            return LogicalDevice(
                handle = VkDevice(deviceBuffer[0], device.handle, deviceCreateInfo),
                physicalDevice = device
            )
        }
    }
}
