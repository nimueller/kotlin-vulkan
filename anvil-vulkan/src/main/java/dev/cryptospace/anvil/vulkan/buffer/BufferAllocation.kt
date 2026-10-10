package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.util.vma.Vma
import java.nio.ByteBuffer

/**
 * Represents a Vulkan buffer allocation with its associated memory.
 * Manages the lifecycle of allocated Vulkan buffer resources using VMA (Vulkan Memory Allocator).
 *
 * Compared by identity: allocations are tracked in sets, and their mapped contents change constantly.
 */
class BufferAllocation(
    /**
     * The VMA allocator responsible for managing this buffer's memory.
     */
    private val allocator: Allocator,
    /**
     * The Vulkan buffer handle.
     */
    val buffer: VkBuffer,
    /**
     * The VMA allocation handle representing the allocated memory.
     */
    val memory: VmaAllocation,
    /**
     * The size of the allocated buffer in bytes.
     */
    val size: Long,
    /**
     * The buffer's memory if it is persistently mapped, valid until the allocation is destroyed.
     * Write with absolute puts, and call [BufferManager.flush] afterwards.
     */
    val mappedData: ByteBuffer?,
) : NativeResource() {

    override fun destroy() {
        // VMA unmaps persistently mapped allocations itself
        Vma.vmaDestroyBuffer(allocator.handle.value, buffer.value, memory.value)
    }

    override fun toString(): String = "BufferAllocation(buffer=$buffer, size=$size)"
}
