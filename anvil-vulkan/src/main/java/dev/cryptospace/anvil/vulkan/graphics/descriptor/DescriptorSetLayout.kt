package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSetLayout
import dev.cryptospace.anvil.vulkan.toBuffer
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout
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
data class DescriptorSetLayout(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorSetLayout,
) : NativeResource() {

    /**
     * Creates a new descriptor set layout with default bindings for vertex and fragment shaders.
     *
     * @param logicalDevice The logical device to create the descriptor set layout on
     */
    constructor(logicalDevice: LogicalDevice) : this(
        logicalDevice,
        createShaderDescriptor(logicalDevice),
    )

    override fun destroy() {
        vkDestroyDescriptorSetLayout(logicalDevice.handle, handle.value, null)
    }

    companion object {

        private fun createShaderDescriptor(logicalDevice: LogicalDevice): VkDescriptorSetLayout =
            MemoryStack.stackPush().use { stack ->
                val layoutBindings = listOf(
                    createVertexShaderDescriptor(stack),
                    createFragmentShaderDescriptor(stack),
                ).toBuffer { size ->
                    VkDescriptorSetLayoutBinding.calloc(size, stack)
                }

                val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    pBindings(layoutBindings)
                }

                val pSetLayout = stack.mallocLong(1)
                vkCreateDescriptorSetLayout(logicalDevice.handle, createInfo, null, pSetLayout)
                    .validateVulkanSuccess("Create descriptor set layout", "Failed to create descriptor set layout")
                VkDescriptorSetLayout(pSetLayout[0])
            }

        private fun createVertexShaderDescriptor(stack: MemoryStack): VkDescriptorSetLayoutBinding =
            VkDescriptorSetLayoutBinding.calloc(stack).apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                descriptorCount(1)
                stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                pImmutableSamplers(null)
            }

        private fun createFragmentShaderDescriptor(stack: MemoryStack): VkDescriptorSetLayoutBinding =
            VkDescriptorSetLayoutBinding.calloc(stack).apply {
                binding(1)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                descriptorCount(1)
                stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            }
    }
}
