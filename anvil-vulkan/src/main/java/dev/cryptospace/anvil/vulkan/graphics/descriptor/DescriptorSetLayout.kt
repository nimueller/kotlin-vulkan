package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSetLayout
import org.lwjgl.vulkan.VK10

/**
 * Represents a Vulkan descriptor set layout that defines the interface between shader stages and resources.
 *
 * This class manages the creation and lifecycle of a VkDescriptorSetLayout object, which describes
 * the layout of resources (such as uniform buffers and combined image samplers) that shaders can access.
 *
 * @property handle The native Vulkan handle to the descriptor set layout
 */
class DescriptorSetLayout(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorSetLayout,
) : NativeResource() {

    override fun destroy() {
        VK10.vkDestroyDescriptorSetLayout(logicalDevice.handle, handle.value, null)
    }
}
