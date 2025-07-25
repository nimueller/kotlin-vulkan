package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT

enum class BufferProperties(
    override val bitmask: Int,
) : BitmaskEnum {

    DEVICE_LOCAL(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    HOST_VISIBLE(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
    HOST_COHERENT(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
}
