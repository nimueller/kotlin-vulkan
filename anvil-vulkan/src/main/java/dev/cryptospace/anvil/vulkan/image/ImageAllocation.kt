package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.buffer.Allocator
import dev.cryptospace.anvil.vulkan.buffer.VmaAllocation
import dev.cryptospace.anvil.vulkan.handle.VkImage
import org.lwjgl.util.vma.Vma

data class ImageAllocation(
    private val allocator: Allocator,
    val image: VkImage,
    val memory: VmaAllocation,
    val width: Int,
    val height: Int,
) : NativeResource() {

    override fun destroy() {
        Vma.vmaDestroyImage(allocator.handle.value, image.value, memory.value)
    }
}
