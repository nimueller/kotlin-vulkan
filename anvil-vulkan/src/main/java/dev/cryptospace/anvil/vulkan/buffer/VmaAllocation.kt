package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.toHexString

@JvmInline
value class VmaAllocation(
    val value: Long,
) {
    override fun toString(): String = value.toHexString()
}
