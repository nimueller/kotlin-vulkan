package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Represents a Vulkan buffer handle.
 * Wraps the native Vulkan buffer handle (VkBuffer) as a 64-bit integer value.
 * Used for identifying and managing buffer resources in the Vulkan graphics API.
 *
 * @property value The native Vulkan buffer handle as a 64-bit integer
 */
@JvmInline
value class VkBuffer(
    val value: Long,
) {

    override fun toString(): String = value.asHexString()
}
