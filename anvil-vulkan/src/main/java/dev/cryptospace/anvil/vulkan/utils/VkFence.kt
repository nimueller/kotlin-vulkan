package dev.cryptospace.anvil.vulkan.utils

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Wrapper class for a Vulkan fence handle.
 *
 * A fence is a synchronization primitive used to track the completion of operations in a
 * Vulkan queue. The fence can be signaled by the GPU and waited upon by the CPU to ensure
 * specific operations have completed.
 *
 * @property value The native Vulkan fence handle as a 64-bit integer
 */
@JvmInline
value class VkFence(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
