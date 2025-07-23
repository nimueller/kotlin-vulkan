package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.pushStringList
import dev.cryptospace.anvil.vulkan.device.suitable.SupportsRequiredExtensionsCriteria
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateDevice
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures

object LogicalDeviceFactory {

    @JvmStatic
    private val logger = logger<LogicalDevice>()

    fun create(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): LogicalDevice = MemoryStack.stackPush().use { stack ->
        val device = deviceSurfaceInfo.physicalDevice
        val graphicsQueueFamily = device.graphicsQueueFamilyIndex
        check(graphicsQueueFamily >= 0) { "Got an invalid graphics queue family index" }

        val presentQueueFamily = deviceSurfaceInfo.presentQueueFamilyIndex
        check(presentQueueFamily >= 0) { "Got an invalid present_queue family index" }

        val queueCreateInfo = buildQueueCreateInfo(stack, graphicsQueueFamily, presentQueueFamily)
        val features = VkPhysicalDeviceFeatures.calloc(stack)
        val deviceCreateInfo = buildDeviceCreateInfo(stack, queueCreateInfo, features)
        val devicePointer = buildDevice(stack, device, deviceCreateInfo)

        return LogicalDevice(
            handle = VkDevice(devicePointer[0], device.handle, deviceCreateInfo),
            physicalDeviceSurfaceInfo = deviceSurfaceInfo,
        )
    }

    private fun buildQueueCreateInfo(
        stack: MemoryStack,
        graphicsQueueFamily: Int,
        presentQueueFamily: Int,
    ): VkDeviceQueueCreateInfo.Buffer {
        val uniqueIndices = setOf(graphicsQueueFamily, presentQueueFamily)
        val queueCreateInfoPointer = VkDeviceQueueCreateInfo.malloc(uniqueIndices.size, stack)

        logger.info("Found unique queue family indices: $uniqueIndices")

        for (queueFamilyIndex in uniqueIndices) {
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(queueFamilyIndex)
                .pQueuePriorities(stack.floats(0.0f))
                .flags(0)

            queueCreateInfoPointer.put(queueCreateInfo)
        }

        queueCreateInfoPointer.flip()
        return queueCreateInfoPointer
    }

    private fun buildDeviceCreateInfo(
        stack: MemoryStack,
        queueCreateInfoBuffer: VkDeviceQueueCreateInfo.Buffer,
        features: VkPhysicalDeviceFeatures,
    ): VkDeviceCreateInfo {
        val enabledExtensions = stack.pushStringList(SupportsRequiredExtensionsCriteria.requiredExtensionNames)

        val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pQueueCreateInfos(queueCreateInfoBuffer)
            .ppEnabledLayerNames(stack.mallocPointer(0))
            .ppEnabledExtensionNames(enabledExtensions)
            .pEnabledFeatures(features)
        return deviceCreateInfo
    }

    private fun buildDevice(
        stack: MemoryStack,
        device: PhysicalDevice,
        deviceCreateInfo: VkDeviceCreateInfo,
    ): PointerBuffer {
        val devicePointer = stack.mallocPointer(1)
        val result = vkCreateDevice(device.handle, deviceCreateInfo, null, devicePointer)
        check(result == VK_SUCCESS) { "Failed to create logical device: $result" }
        logger.info("Created logical device for ${device.name}")
        return devicePointer
    }
}
