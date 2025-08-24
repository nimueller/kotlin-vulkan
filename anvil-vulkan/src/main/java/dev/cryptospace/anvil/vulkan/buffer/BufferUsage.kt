package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum
import org.lwjgl.vulkan.VK10

/**
 * Represents possible usage flags for Vulkan buffers.
 * These flags determine how a buffer can be used in the graphics pipeline.
 * Multiple flags can be combined using binary OR operations to specify multiple uses for a single buffer.
 */
enum class BufferUsage(
    override val bitmask: Int,
) : BitmaskEnum {

    /** Buffer can be used as a vertex buffer containing vertex data for mesh rendering. */
    VERTEX_BUFFER(VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),

    /** Buffer can be used as an index buffer containing mesh indices for indexed drawing. */
    INDEX_BUFFER(VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT),

    /** Buffer can be used as a source for memory transfer operations. */
    TRANSFER_SRC(VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT),

    /** Buffer can be used as a destination for memory transfer operations. */
    TRANSFER_DST(VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT),

    /** Buffer can be used to store uniform data accessible by shaders. */
    UNIFORM_BUFFER(VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
}
