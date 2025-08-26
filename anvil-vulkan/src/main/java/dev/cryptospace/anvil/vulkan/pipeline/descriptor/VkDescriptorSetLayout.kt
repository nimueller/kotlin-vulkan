package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.core.native.asHexString

/**
 * A wrapper class for a Vulkan descriptor set layout handle.
 * Represents a Vulkan descriptor set layout object that
 * defines the interface between shader stages and shader resources.
 */
@JvmInline
value class VkDescriptorSetLayout(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
