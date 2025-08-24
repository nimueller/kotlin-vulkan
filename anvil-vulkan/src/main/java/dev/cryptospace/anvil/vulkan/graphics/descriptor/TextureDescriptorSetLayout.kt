package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSet
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT
import org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT
import org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfo
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkDescriptorSetVariableDescriptorCountAllocateInfo

/**
 * Represents a Vulkan descriptor set layout specifically designed for material textures in the fragment shader.
 *
 * This class manages the creation and lifecycle of a VkDescriptorSetLayout object that describes
 * the layout of combined image samplers used for material textures. It creates a single binding
 * at index 1 that can be accessed in the fragment shader stage.
 *
 * @property handle The native Vulkan handle to the descriptor set layout
 */
class TextureDescriptorSetLayout(
    logicalDevice: LogicalDevice,
) : DescriptorSetLayout(
    logicalDevice = logicalDevice,
    handle = MemoryStack.stackPush().use { stack ->
        createDescriptorSetLayout(
            logicalDevice = logicalDevice,
            stack = stack,
            bindings = listOf(createFragmentShaderDescriptor(stack)),
            createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                pNext(createFlagsInfo(stack))
                flags(VK12.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT)
            },
        )
    },
) {

    override fun createDescriptorSet(descriptorPool: DescriptorPool, count: Int): DescriptorSet {
        MemoryStack.stackPush().use { stack ->
            check(count > 0) { "Count must be greater than 0" }

            val setLayouts = stack.mallocLong(count)

            repeat(count) {
                setLayouts.put(handle.value)
            }

            setLayouts.flip()

            val countInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack).apply {
                sType(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO)
                pDescriptorCounts(stack.ints(512))
            }

            val allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                pNext(countInfo)
                descriptorPool(descriptorPool.handle.value)
                pSetLayouts(setLayouts)
            }

            val pDescriptorSets = stack.mallocLong(count)
            vkAllocateDescriptorSets(logicalDevice.handle, allocateInfo, pDescriptorSets)
                .validateVulkanSuccess("Allocate descriptor sets", "Failed to allocate descriptor sets")
            val handles = List(count) {
                VkDescriptorSet(pDescriptorSets[it])
            }
            return DescriptorSet(handles)
        }
    }

    companion object {

        const val MAX_TEXTURE_COUNT = 1024

        private fun createFragmentShaderDescriptor(stack: MemoryStack): VkDescriptorSetLayoutBinding =
            VkDescriptorSetLayoutBinding.calloc(stack).apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                descriptorCount(MAX_TEXTURE_COUNT)
                stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            }

        private fun createFlagsInfo(stack: MemoryStack) =
            VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack).apply {
                sType(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
                bindingCount(1)
                pBindingFlags(
                    stack.ints(
                        VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT or
                            VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT or
                            VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT,
                    ),
                )
            }
    }
}
