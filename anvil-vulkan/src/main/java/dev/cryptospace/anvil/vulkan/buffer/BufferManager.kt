package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum.Companion.toBitmask
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.Fence
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import java.nio.ByteBuffer
import java.util.*

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

    private val buffers = mutableListOf<BufferAllocation>()

    /**
     * Allocates a Vulkan buffer with the specified size, usage flags, and memory properties.
     *
     * @param size The size of the buffer in bytes
     * @param usage Set of buffer usage flags indicating how the buffer will be used
     * @param preferredProperties Set of memory property flags specifying preferred memory characteristics
     * @return A BufferAllocation containing the created buffer and its associated memory
     */
    fun allocateBuffer(
        size: Long,
        usage: EnumSet<BufferUsage>,
        preferredProperties: EnumSet<BufferProperties>? = null,
    ): BufferAllocation = MemoryStack.stackPush().use { stack ->
        val bufferInfo = VkBufferCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            size(size)
            usage(usage.toBitmask())
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }

        val allocationInfo = VmaAllocationCreateInfo.calloc(stack).apply {
            usage(Vma.VMA_MEMORY_USAGE_AUTO)

            if (preferredProperties != null) {
                preferredFlags(preferredProperties.toBitmask())
            }
        }

        val pBuffer = stack.mallocLong(1)
        val pAllocation = stack.mallocPointer(1)

        Vma.vmaCreateBuffer(allocator.handle.value, bufferInfo, allocationInfo, pBuffer, pAllocation, null)
            .validateVulkanSuccess("Create vertex buffer", "Failed to create buffer for vertices")

        val buffer = VkBuffer(pBuffer[0])
        val memory = VmaAllocation(pAllocation[0])

        return BufferAllocation(allocator, buffer, memory, size).also { bufferAllocation ->
            buffers.add(bufferAllocation)
            logger.info("Allocated buffer $buffer size $size with memory $memory")
        }
    }

    /**
     * Uploads arbitrary data to a Vulkan buffer allocation.
     * The data is copied from the provided ByteBuffer to the allocated GPU memory.
     *
     * @param allocation The buffer allocation to upload data to, containing both buffer and memory handles
     * @param data The ByteBuffer containing the data to upload
     */
    fun uploadData(allocation: BufferAllocation, data: ByteBuffer) {
        check(allocation.size == data.remaining().toLong()) {
            error("Buffer size mismatch: expected ${allocation.size} but was ${data.remaining()}")
        }

        MemoryStack.stackPush().use { stack ->
            val verticesBufferAddress = MemoryUtil.memAddress(data)

            val pMemory = stack.mallocPointer(1)
            Vma.vmaMapMemory(allocator.handle.value, allocation.memory.value, pMemory)
                .validateVulkanSuccess("Map buffer memory", "Failed to map memory for uploading data")
            MemoryUtil.memCopy(verticesBufferAddress, pMemory[0], allocation.size)
            Vma.vmaUnmapMemory(allocator.handle.value, allocation.memory.value)
        }
    }

    /**
     * Creates a temporary staging buffer in host-visible memory for efficient data transfer to device-local memory.
     *
     * This method performs the following operations:
     * 1. Allocates a staging buffer with TRANSFER_SRC usage in host-visible memory
     * 2. Uploads provided data from the ByteBuffer to the staging buffer
     * 3. Executes the provided block with the staging buffer and synchronization fence
     * 4. Waits for the fence to signal operation completion
     * 5. Automatically destroys the staging buffer and frees associated memory
     *
     * The staging buffer is optimized for host-to-device transfers and is allocated with
     * HOST_VISIBLE and HOST_COHERENT properties for efficient memory mapping.
     *
     * @param bytes The ByteBuffer containing the data to be staged (must be direct for memory mapping)
     * @param block Lambda that receives the staging buffer and fence for synchronization operations
     * @return The result of type T from the block execution
     * @throws IllegalArgumentException If the provided ByteBuffer is not direct
     */
    // TODO : use a pool of staging buffers to avoid excessive memory allocations
    fun <T> withStagingBuffer(bytes: ByteBuffer, block: (stagingBuffer: BufferAllocation, fence: Fence) -> T): T {
        check(bytes.isDirect) { "ByteBuffer must be direct" }
        val stagingBuffer = allocateBuffer(
            size = bytes.remaining().toLong(),
            usage = EnumSet.of(BufferUsage.TRANSFER_SRC),
            preferredProperties = EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
        )

        Fence(logicalDevice).use { fence ->
            try {
                uploadData(stagingBuffer, bytes)
                val value = block(stagingBuffer, fence)
                VK10.vkWaitForFences(logicalDevice.handle, fence.handle.value, true, Long.MAX_VALUE)
                return value
            } finally {
                stagingBuffer.close()
                buffers.remove(stagingBuffer)
            }
        }
    }

    /**
     * Copies data from one buffer to another using a command buffer.
     *
     * @param source The source buffer allocation to copy from
     * @param destination The destination buffer allocation to copy to
     * @throws IllegalStateException If source and destination buffer sizes don't match
     */
    fun transferBuffer(source: BufferAllocation, destination: BufferAllocation, fence: Fence) {
        check(source.size == destination.size) {
            error("Buffer size mismatch: expected ${source.size} but was ${destination.size}")
        }

        // TODO : use dedicated transfer queue for this
        commandPool.recordSingleTimeCommands(logicalDevice.graphicsQueue, fence) { stack, commandBuffer ->
            val bufferCopy = VkBufferCopy.calloc(stack).apply {
                srcOffset(0)
                dstOffset(0)
                size(source.size)
            }

            val bufferCopies = VkBufferCopy.calloc(1, stack)
                .put(bufferCopy)
                .flip()

            vkCmdCopyBuffer(
                commandBuffer,
                source.buffer.value,
                destination.buffer.value,
                bufferCopies,
            )
        }
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
        private val logger = logger<BufferManager>()
    }
}
