package dev.cryptospace.anvil.vulkan.pipeline.shader

import org.lwjgl.vulkan.VK10

/**
 * Represents different stages in the graphics pipeline where shaders can be executed.
 * These stages define the specific processing steps that geometry and pixel data go through
 * during rendering.
 *
 * @property vkValue The Vulkan shader stage flag bit representing this shader stage.
 */
enum class ShaderStage(
    val vkValue: Int,
) {

    /**
     * The vertex shader stage processes individual vertices.
     * This stage handles vertex transformations, positions, and per-vertex attributes.
     */
    VERTEX(VK10.VK_SHADER_STAGE_VERTEX_BIT),

    /**
     * The fragment shader stage processes individual fragments (pixels).
     * This stage determines the final color and other attributes of each pixel.
     */
    FRAGMENT(VK10.VK_SHADER_STAGE_FRAGMENT_BIT),

    ;

    companion object {
        fun Collection<ShaderStage>.toBitmask(): Int = this
            .map { stage -> stage.vkValue }
            .reduce { acc, stageBit -> acc or stageBit }
    }
}
