package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.pushStringList
import dev.cryptospace.anvil.vulkan.device.suitable.SupportsRequiredExtensionsCriteria
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceDescriptorIndexingFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures

/**
 * Factory object responsible for creating Vulkan logical devices.
 * Handles the creation of logical devices with appropriate queue families,
 * features, and extensions based on the provided physical device information.
 */
object LogicalDeviceFactory {

    @JvmStatic
    private val log = logger<LogicalDevice>()

    /**
     * Creates a new logical device based on the provided physical device surface information.
     *
     * @param deviceSurfaceInfo Information about the physical device and its surface capabilities
     * @return A new [LogicalDevice] instance configured with appropriate queues and features
     * @throws IllegalStateException if queue family indices are invalid
     */
    fun create(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): LogicalDevice = MemoryStack.stackPush().use { stack ->
        val device = deviceSurfaceInfo.physicalDevice
        val graphicsQueueFamily = device.graphicsQueueFamilyIndex
        check(graphicsQueueFamily >= 0) { "Got an invalid graphics queue family index" }

        val presentQueueFamily = deviceSurfaceInfo.presentQueueFamilyIndex
        check(presentQueueFamily >= 0) { "Got an invalid present_queue family index" }

        val queueCreateInfo = buildQueueCreateInfo(stack, graphicsQueueFamily, presentQueueFamily)
        val features = VkPhysicalDeviceFeatures.calloc(stack).apply {
            // only enable anisotropy if it is supported
            if (deviceSurfaceInfo.physicalDevice.features.samplerAnisotropy()) {
                samplerAnisotropy(true)
            }
        }
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

        log.info("Found unique queue family indices: $uniqueIndices")

        for (queueFamilyIndex in uniqueIndices) {
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
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
        val indexingFeatures = buildIndexingFeatures(stack)

        val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
            .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pNext(indexingFeatures)
            .pQueueCreateInfos(queueCreateInfoBuffer)
            .ppEnabledLayerNames(stack.mallocPointer(0))
            .ppEnabledExtensionNames(enabledExtensions)
            .pEnabledFeatures(features)
        return deviceCreateInfo
    }

    private fun buildIndexingFeatures(stack: MemoryStack) =
        VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack).apply {
            sType(VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES)
            runtimeDescriptorArray(true)
            shaderInputAttachmentArrayDynamicIndexing(true)
            descriptorBindingVariableDescriptorCount(true)
            descriptorBindingPartiallyBound(true)
            descriptorBindingSampledImageUpdateAfterBind(true)
        }

    private fun buildDevice(
        stack: MemoryStack,
        device: PhysicalDevice,
        deviceCreateInfo: VkDeviceCreateInfo,
    ): PointerBuffer {
        val devicePointer = stack.mallocPointer(1)
        VK10.vkCreateDevice(device.handle, deviceCreateInfo, null, devicePointer)
            .validateVulkanSuccess("Create logical device", "Failed to create logical device")
        log.info("Created logical device for ${device.name}")
        return devicePointer
    }
}
