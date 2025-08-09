package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.util.vma.Vma

data class BufferAllocation(
    private val allocator: Allocator,
    val buffer: VkBuffer,
    val memory: VmaAllocation,
    val size: Long,
) : NativeResource() {

    override fun destroy() {
        Vma.vmaDestroyBuffer(allocator.handle.value, buffer.value, memory.value)
    }
}
