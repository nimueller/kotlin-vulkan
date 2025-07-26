package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT

enum class BufferUsage(
    override val bitmask: Int,
) : BitmaskEnum {

    VERTEX_BUFFER(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
    INDEX_BUFFER(VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
    TRANSFER_SRC(VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
    TRANSFER_DST(VK_BUFFER_USAGE_TRANSFER_DST_BIT),
    UNIFORM_BUFFER(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
}
