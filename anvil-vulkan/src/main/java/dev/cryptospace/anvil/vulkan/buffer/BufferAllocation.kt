package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.util.vma.Vma

/**
 * Represents a Vulkan buffer allocation with its associated memory.
 * Manages the lifecycle of allocated Vulkan buffer resources using VMA (Vulkan Memory Allocator).
 */
data class BufferAllocation(
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
) : NativeResource() {

    override fun destroy() {
        Vma.vmaDestroyBuffer(allocator.handle.value, buffer.value, memory.value)
    }
}
