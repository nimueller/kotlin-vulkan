package dev.cryptospace.anvil.vulkan.pipeline.shader

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Represents a Vulkan shader module handle.
 * A shader module contains shader code and associated resources used in the graphics pipeline.
 * The value represents the native Vulkan handle as a 64-bit integer.
 */
@JvmInline
value class VkShaderModule(
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
