package dev.cryptospace.anvil.vulkan.pipeline.descriptor

/**
 * Represents a collection of Vulkan descriptor sets that bind resources to shader bindings.
 * Descriptor sets contain the actual binding information between shader resources
 * (like uniform buffers and samplers) and the shader binding points.
 *
 * @property logicalDevice The logical device this descriptor set belongs to
 * @property handles List of native Vulkan descriptor set handles
 */
class DescriptorSet(
    val handles: List<VkDescriptorSet>,
) {

    operator fun get(index: Int): VkDescriptorSet = handles[index]
}
