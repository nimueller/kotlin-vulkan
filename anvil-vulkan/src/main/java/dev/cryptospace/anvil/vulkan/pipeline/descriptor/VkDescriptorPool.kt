package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Wrapper class for a Vulkan descriptor pool handle.
 *
 * A descriptor pool maintains a pool of descriptors, from which descriptor sets are allocated.
 *
 * @property value The native handle to the Vulkan descriptor pool.
 */
@JvmInline
value class VkDescriptorPool(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
