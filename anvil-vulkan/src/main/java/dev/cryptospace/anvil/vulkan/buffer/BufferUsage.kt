package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT

enum class BufferUsage(
    override val bitmask: Int,
) : BitmaskEnum {

    VERTEX(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
    STAGING_SOURCE(VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
    STAGING_DESTINATION(VK_BUFFER_USAGE_TRANSFER_DST_BIT),
}
