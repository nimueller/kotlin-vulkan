package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum.Companion.toBitmask
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.utils.Fence
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCmdCopyBuffer
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import java.nio.ByteBuffer
import java.util.EnumSet

/**
 * Manages Vulkan buffer resources and memory allocation.
 *
 * This class handles the creation, management, and cleanup of Vulkan buffers
 * and their associated memory. It provides methods for creating vertex buffers
 * and uploading vertex data to GPU memory.
 *
 * @property logicalDevice The logical device used for buffer operations
 */
class BufferManager(
    private val allocator: Allocator,
    private val logicalDevice: LogicalDevice,
    private val commandPool: CommandPool,
) : NativeResource() {

    private val buffers = mutableSetOf<BufferAllocation>()

    /**
     * A copy from an offset in one buffer to the start of another.
     */
    class CopyRegion(
        val sourceOffset: Long,
        val destination: BufferAllocation,
        val size: Long,
    )

    /**
     * Allocates a Vulkan buffer with the specified size, usage flags, and memory properties.
     *
     * @param size The size of the buffer in bytes
     * @param usage Set of buffer usage flags indicating how the buffer will be used
     * @param preferredProperties Set of memory property flags specifying preferred memory characteristics.
     *   Including [BufferProperties.HOST_VISIBLE] makes the buffer writable from the CPU.
     * @param persistentlyMapped Keep the memory mapped for the buffer's lifetime, see [BufferAllocation.mappedData].
     *   Requires [BufferProperties.HOST_VISIBLE].
     * @return A BufferAllocation containing the created buffer and its associated memory
     */
    fun allocateBuffer(
        size: Long,
        usage: EnumSet<BufferUsage>,
        preferredProperties: EnumSet<BufferProperties>? = null,
        persistentlyMapped: Boolean = false,
    ): BufferAllocation = MemoryStack.stackPush().use { stack ->
        require(size > 0) { "Buffer size must be positive, was $size" }
        val hostVisible = preferredProperties?.contains(BufferProperties.HOST_VISIBLE) == true
        require(!persistentlyMapped || hostVisible) { "Only host visible buffers can be mapped" }

        val bufferInfo = VkBufferCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            size(size)
            usage(usage.toBitmask())
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }

        val allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack).apply {
            usage(Vma.VMA_MEMORY_USAGE_AUTO)

            if (preferredProperties != null) {
                preferredFlags(preferredProperties.toBitmask())
            }

            // With VMA_MEMORY_USAGE_AUTO, memory is only guaranteed to be mappable when host access is declared
            var flags = 0
            if (hostVisible) flags = flags or Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT
            if (persistentlyMapped) flags = flags or Vma.VMA_ALLOCATION_CREATE_MAPPED_BIT
            flags(flags)
        }

        val pBuffer = stack.mallocLong(1)
        val pAllocation = stack.mallocPointer(1)
        val allocationInfo = VmaAllocationInfo.calloc(stack)

        Vma.vmaCreateBuffer(
            allocator.handle.value,
            bufferInfo,
            allocationCreateInfo,
            pBuffer,
            pAllocation,
            allocationInfo,
        )
            .validateVulkanSuccess("Create buffer", "Failed to create buffer of $size bytes")

        val buffer = VkBuffer(pBuffer[0])
        val memory = VmaAllocation(pAllocation[0])
        val mappedData = if (persistentlyMapped) {
            MemoryUtil.memByteBuffer(allocationInfo.pMappedData(), Math.toIntExact(size))
        } else {
            null
        }

        return BufferAllocation(allocator, buffer, memory, size, mappedData).also { bufferAllocation ->
            buffers.add(bufferAllocation)
            log.debug("Allocated buffer {} size {} with memory {}", buffer, size, memory)
        }
    }

    /**
     * Makes CPU writes to a host visible allocation visible to the GPU. A no-op for host coherent memory.
     */
    fun flush(allocation: BufferAllocation) {
        Vma.vmaFlushAllocation(allocator.handle.value, allocation.memory.value, 0, VK10.VK_WHOLE_SIZE)
    }

    /**
     * Lets [write] fill a host visible allocation through a mapping, then flushes it.
     */
    private fun writeMapped(allocation: BufferAllocation, write: (ByteBuffer) -> Unit) {
        val persistent = allocation.mappedData
        if (persistent != null) {
            write(persistent)
        } else {
            MemoryStack.stackPush().use { stack ->
                val pMemory = stack.mallocPointer(1)
                Vma.vmaMapMemory(allocator.handle.value, allocation.memory.value, pMemory)
                    .validateVulkanSuccess("Map buffer memory", "Failed to map memory for uploading data")
                try {
                    write(MemoryUtil.memByteBuffer(pMemory[0], Math.toIntExact(allocation.size)))
                } finally {
                    Vma.vmaUnmapMemory(allocator.handle.value, allocation.memory.value)
                }
            }
        }
        flush(allocation)
    }

    /**
     * Creates a temporary staging buffer in host-visible memory for efficient data transfer to device-local memory.
     *
     * This method performs the following operations:
     * 1. Allocates a staging buffer with TRANSFER_SRC usage in host-visible memory
     * 2. Uploads provided data from the ByteBuffer to the staging buffer
     * 3. Executes the provided block with the staging buffer and synchronization fence
     * 4. Waits for the fence, so the GPU is done with the staging buffer
     * 5. Automatically destroys the staging buffer and frees associated memory
     *
     * @param bytes The data to upload, must be a direct buffer
     * @param block Lambda that receives the staging buffer and fence for synchronization operations
     * @return The result of type T from the block execution
     */
    fun <T> withStagingBuffer(bytes: ByteBuffer, block: (stagingBuffer: BufferAllocation, fence: Fence) -> T): T {
        require(bytes.isDirect) { "ByteBuffer must be direct" }
        val size = bytes.remaining().toLong()
        return withStagingBuffer(
            size = size,
            fill = { mapped -> MemoryUtil.memCopy(MemoryUtil.memAddress(bytes), MemoryUtil.memAddress(mapped), size) },
            block = block,
        )
    }

    /**
     * Like [withStagingBuffer], but lets [fill] write directly into the mapped staging memory,
     * which avoids an intermediate copy of the data.
     *
     * @param fill Receives the staging memory, positioned at 0 with exactly [size] bytes remaining
     */
    // TODO : use a pool of staging buffers to avoid excessive memory allocations
    fun <T> withStagingBuffer(
        size: Long,
        fill: (ByteBuffer) -> Unit,
        block: (stagingBuffer: BufferAllocation, fence: Fence) -> T,
    ): T {
        val stagingBuffer = allocateBuffer(
            size = size,
            usage = EnumSet.of(BufferUsage.TRANSFER_SRC),
            preferredProperties = EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
            persistentlyMapped = true,
        )

        try {
            writeMapped(stagingBuffer, fill)

            return Fence(logicalDevice).use { fence ->
                val value = block(stagingBuffer, fence)
                VK10.vkWaitForFences(logicalDevice.handle, fence.handle.value, true, Long.MAX_VALUE)
                    .validateVulkanSuccess("Wait for upload", "Failed waiting for an upload to finish")
                value
            }
        } finally {
            freeBuffer(stagingBuffer)
        }
    }

    /**
     * Copies data from one buffer to another using a command buffer.
     *
     * @param source The source buffer allocation to copy from
     * @param destination The destination buffer allocation to copy to
     * @throws IllegalArgumentException If source and destination buffer sizes don't match
     */
    fun transferBuffer(source: BufferAllocation, destination: BufferAllocation, fence: Fence) {
        require(source.size == destination.size) {
            "Buffer size mismatch: expected ${source.size} but was ${destination.size}"
        }

        copyBuffer(source, listOf(CopyRegion(0, destination, source.size)), fence)
    }

    /**
     * Copies several regions of [source] into other buffers with a single submission.
     */
    fun copyBuffer(source: BufferAllocation, regions: List<CopyRegion>, fence: Fence) {
        regions.forEach { region ->
            require(region.sourceOffset + region.size <= source.size && region.size <= region.destination.size) {
                "Copy region of ${region.size} bytes at ${region.sourceOffset} does not fit the buffers"
            }
        }

        // TODO : use dedicated transfer queue for this
        commandPool.recordSingleTimeCommands(logicalDevice.graphicsQueue, fence) { stack, commandBuffer ->
            for (region in regions) {
                val bufferCopies = VkBufferCopy.calloc(1, stack)
                bufferCopies[0]
                    .srcOffset(region.sourceOffset)
                    .dstOffset(0)
                    .size(region.size)

                vkCmdCopyBuffer(commandBuffer, source.buffer.value, region.destination.buffer.value, bufferCopies)
            }
        }
    }

    /**
     * Destroys a buffer immediately. The caller must ensure the GPU no longer uses it.
     */
    fun freeBuffer(allocation: BufferAllocation) {
        check(buffers.remove(allocation)) { "Buffer ${allocation.buffer} is not managed by this BufferManager" }
        allocation.close()
    }

    /**
     * Destroys all buffer resources managed by this BufferManager.
     */
    override fun destroy() {
        buffers.forEach { buffer -> buffer.close() }
        buffers.clear()
    }

    companion object {

        @JvmStatic
        private val log = logger<BufferManager>()
    }
}
