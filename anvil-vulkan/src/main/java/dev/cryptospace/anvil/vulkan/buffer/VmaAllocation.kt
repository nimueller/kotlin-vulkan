package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.asHexString

@JvmInline
value class VmaAllocation(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
