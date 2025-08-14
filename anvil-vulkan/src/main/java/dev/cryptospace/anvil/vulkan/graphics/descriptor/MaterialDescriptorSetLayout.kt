package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding

/**
 * Represents a Vulkan descriptor set layout specifically designed for material textures in the fragment shader.
 *
 * This class manages the creation and lifecycle of a VkDescriptorSetLayout object that describes
 * the layout of combined image samplers used for material textures. It creates a single binding
 * at index 1 that can be accessed in the fragment shader stage.
 *
 * @property handle The native Vulkan handle to the descriptor set layout
 */
class MaterialDescriptorSetLayout(
    logicalDevice: LogicalDevice,
) : DescriptorSetLayout(
    logicalDevice = logicalDevice,
    handle = MemoryStack.stackPush().use { stack ->
        createDescriptorSetLayout(logicalDevice, stack, listOf(createFragmentShaderDescriptor(stack)))
    },
) {

    companion object {

        private fun createFragmentShaderDescriptor(stack: MemoryStack): VkDescriptorSetLayoutBinding =
            VkDescriptorSetLayoutBinding.calloc(stack).apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                descriptorCount(1)
                stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            }
    }
}
