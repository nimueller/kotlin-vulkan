package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Represents a Vulkan descriptor set handle.
 *
 * A descriptor set is a collection of resources (like buffers, images, etc.) that can be bound
 * for use in shader programs. This class provides a type-safe wrapper around the raw Vulkan handle.
 *
 * @property value The raw Vulkan handle value for the descriptor set.
 */
@JvmInline
value class VkDescriptorSet(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
