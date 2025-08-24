package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.native.asHexString

/**
 * Wrapper class for a Vulkan pipeline layout handle.
 *
 * A pipeline layout defines the interface between shader resources and shader code,
 * specifying the layout of descriptor sets and push constants used by the shaders.
 */
@JvmInline
value class VkPipelineLayout(
    /** The native Vulkan handle to the pipeline layout object */
    val value: Long,
) {
    override fun toString(): String = value.asHexString()
}
