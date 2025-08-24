package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum
import org.lwjgl.vulkan.VK10

/**
 * Represents memory property flags for Vulkan buffers.
 * These properties determine the memory allocation and access characteristics of buffers.
 *
 * @property bitmask The Vulkan memory property flag bits
 */
enum class BufferProperties(
    override val bitmask: Int,
) : BitmaskEnum {

    /**
     * Memory allocated with this flag is device local.
     * Device local memory may be more efficient for device access but may not be directly accessible by the host.
     */
    DEVICE_LOCAL(VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),

    /**
     * Memory allocated with this flag can be mapped for host access.
     * The host can read and write this memory directly.
     */
    HOST_VISIBLE(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),

    /**
     * Host memory accesses to mapped memory are automatically coherent.
     * No explicit flush or invalidate commands are needed.
     */
    HOST_COHERENT(VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
}
