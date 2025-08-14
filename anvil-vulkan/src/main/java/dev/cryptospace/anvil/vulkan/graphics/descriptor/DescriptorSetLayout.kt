package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSetLayout
import dev.cryptospace.anvil.vulkan.toBuffer
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

/**
 * Represents a Vulkan descriptor set layout that defines the interface between shader stages and resources.
 *
 * This class manages the creation and lifecycle of a VkDescriptorSetLayout object, which describes
 * the layout of resources (such as uniform buffers and combined image samplers) that shaders can access.
 *
 * @property handle The native Vulkan handle to the descriptor set layout
 */
abstract class DescriptorSetLayout(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorSetLayout,
) : NativeResource() {

    override fun destroy() {
        VK10.vkDestroyDescriptorSetLayout(logicalDevice.handle, handle.value, null)
    }

    companion object {

        fun createDescriptorSetLayout(
            logicalDevice: LogicalDevice,
            stack: MemoryStack,
            bindings: List<VkDescriptorSetLayoutBinding>,
        ): VkDescriptorSetLayout {
            val layoutBindings = bindings.toBuffer { size ->
                VkDescriptorSetLayoutBinding.calloc(size, stack)
            }

            val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                pBindings(layoutBindings)
            }

            val pSetLayout = stack.mallocLong(1)
            VK10.vkCreateDescriptorSetLayout(logicalDevice.handle, createInfo, null, pSetLayout)
                .validateVulkanSuccess("Create descriptor set layout", "Failed to create descriptor set layout")
            return VkDescriptorSetLayout(pSetLayout[0])
        }
    }
}
