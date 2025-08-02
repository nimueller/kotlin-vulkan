package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum.Companion.toBitmask
import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY
import org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO
import org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers
import org.lwjgl.vulkan.VK10.vkAllocateMemory
import org.lwjgl.vulkan.VK10.vkBeginCommandBuffer
import org.lwjgl.vulkan.VK10.vkBindBufferMemory
import org.lwjgl.vulkan.VK10.vkBindImageMemory
import org.lwjgl.vulkan.VK10.vkCmdCopyBuffer
import org.lwjgl.vulkan.VK10.vkCreateBuffer
import org.lwjgl.vulkan.VK10.vkEndCommandBuffer
import org.lwjgl.vulkan.VK10.vkFreeCommandBuffers
import org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements
import org.lwjgl.vulkan.VK10.vkGetImageMemoryRequirements
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VK10.vkMapMemory
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkQueueWaitIdle
import org.lwjgl.vulkan.VK10.vkUnmapMemory
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VkSubmitInfo
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
    private val logicalDevice: LogicalDevice,
) : NativeResource() {

    private val buffers = mutableListOf<BufferAllocation>()

    /**
     * Allocates a Vulkan buffer with the specified size, usage flags, and memory properties.
     *
     * @param size The size of the buffer in bytes
     * @param usage Set of buffer usage flags indicating how the buffer will be used
     * @param properties Set of memory property flags specifying required memory characteristics
     * @return A BufferAllocation containing the created buffer and its associated memory
     */
    fun allocateBuffer(
        size: Long,
        usage: EnumSet<BufferUsage>,
        properties: EnumSet<BufferProperties>,
    ): BufferAllocation {
        // Create the buffer
        val buffer = MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                size(size)
                usage(usage.toBitmask())
                sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            }

            val pBuffer = stack.mallocLong(1)
            vkCreateBuffer(logicalDevice.handle, bufferInfo, null, pBuffer)
                .validateVulkanSuccess("Create vertex buffer", "Failed to create buffer for vertices")
            VkBuffer(pBuffer[0])
        }

        // Allocate and bind memory
        val memory = MemoryStack.stackPush().use { stack ->
            val bufferMemoryRequirements = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(logicalDevice.handle, buffer.value, bufferMemoryRequirements)

            val memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                allocationSize(bufferMemoryRequirements.size())
                memoryTypeIndex(
                    findMemoryType(bufferMemoryRequirements.memoryTypeBits(), properties.toBitmask()),
                )
            }

            val pDeviceMemory = stack.mallocLong(1)
            vkAllocateMemory(logicalDevice.handle, memoryAllocateInfo, null, pDeviceMemory)
                .validateVulkanSuccess("Allocate buffer memory", "Failed to allocate memory for vertex buffer")
            vkBindBufferMemory(logicalDevice.handle, buffer.value, pDeviceMemory[0], 0)
                .validateVulkanSuccess("Bind buffer memory", "Failed to bind memory to vertex buffer")

            VkDeviceMemory(pDeviceMemory[0])
        }

        val allocation = BufferAllocation(logicalDevice, buffer, memory, size)
        buffers.add(allocation)
        logger.debug {
            "Allocated buffer: $buffer with memory: $memory (size: $size) for usage: $usage and properties: $properties"
        }
        return allocation
    }

    fun allocateImageBuffer(textureImage: VkImage): VkDeviceMemory = MemoryStack.stackPush().use { stack ->
        val bufferMemoryRequirements = VkMemoryRequirements.calloc(stack)
        vkGetImageMemoryRequirements(logicalDevice.handle, textureImage.value, bufferMemoryRequirements)

        val memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            allocationSize(bufferMemoryRequirements.size())
            memoryTypeIndex(
                findMemoryType(bufferMemoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
            )
        }

        val pTextureImageMemory = stack.mallocLong(1)
        vkAllocateMemory(logicalDevice.handle, memoryAllocateInfo, null, pTextureImageMemory)
            .validateVulkanSuccess("Allocate buffer memory", "Failed to allocate memory for vertex buffer")
        vkBindImageMemory(logicalDevice.handle, textureImage.value, pTextureImageMemory[0], 0)
            .validateVulkanSuccess("Bind buffer memory", "Failed to bind memory to vertex buffer")
        return VkDeviceMemory(pTextureImageMemory[0])
    }

    private fun findMemoryType(typeFilter: Int, properties: Int): Int = MemoryStack.stackPush().use { stack ->
        val memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
        vkGetPhysicalDeviceMemoryProperties(logicalDevice.physicalDevice.handle, memProperties)

        for (i in 0 until memProperties.memoryTypeCount()) {
            if ((typeFilter and (1 shl i)) != 0 &&
                (memProperties.memoryTypes(i).propertyFlags() and properties) == properties
            ) {
                return i
            }
        }

        error("Failed to find suitable memory type")
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
            vkMapMemory(logicalDevice.handle, allocation.memory.value, 0, allocation.size, 0, pMemory)
                .validateVulkanSuccess("Map vertex buffer memory", "Failed to map memory for uploading vertex data")
            MemoryUtil.memCopy(verticesBufferAddress, pMemory[0], allocation.size)
            vkUnmapMemory(logicalDevice.handle, allocation.memory.value)
        }
    }

    /**
     * Maps the buffer memory and returns a pointer to the mapped memory region.
     *
     * @param allocation The buffer allocation to map memory from
     * @return A native memory pointer to the mapped region
     */
    fun getPointer(allocation: BufferAllocation): Long {
        MemoryStack.stackPush().use { stack ->
            val pMemory = stack.mallocPointer(1)
            vkMapMemory(logicalDevice.handle, allocation.memory.value, 0, allocation.size, 0, pMemory)
            val pointer = pMemory[0]
            return pointer
        }
    }

    /**
     * Copies data from one buffer to another using a command buffer.
     *
     * @param source The source buffer allocation to copy from
     * @param destination The destination buffer allocation to copy to
     * @throws IllegalStateException If source and destination buffer sizes don't match
     */
    fun transferBuffer(source: BufferAllocation, destination: BufferAllocation) {
        check(source.size == destination.size) {
            error("Buffer size mismatch: expected ${source.size} but was ${destination.size}")
        }

        MemoryStack.stackPush().use { stack ->
            val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
                commandPool(logicalDevice.commandPool.handle.value)
            }

            val pCommandBuffers = stack.mallocPointer(1)
            vkAllocateCommandBuffers(logicalDevice.handle, allocateInfo, pCommandBuffers)
                .validateVulkanSuccess("Allocate command buffer", "Failed to allocate command buffer for buffer copy")

            val beginInfo = VkCommandBufferBeginInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            }

            val commandBuffer = VkCommandBuffer(pCommandBuffers[0], logicalDevice.handle)
            vkBeginCommandBuffer(commandBuffer, beginInfo)
                .validateVulkanSuccess("Begin command buffer", "Failed to begin command buffer for buffer copy")

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
            vkEndCommandBuffer(commandBuffer)
                .validateVulkanSuccess("End command buffer", "Failed to end command buffer for buffer copy")

            val queueSubmitInfo = VkSubmitInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                pCommandBuffers(stack.pointers(commandBuffer))
            }

            vkQueueSubmit(logicalDevice.graphicsQueue, queueSubmitInfo, VK_NULL_HANDLE)
                .validateVulkanSuccess("Queue submit", "Failed to submit command buffer for buffer copy")
            vkQueueWaitIdle(logicalDevice.graphicsQueue)
                .validateVulkanSuccess("Queue wait idle", "Failed to wait for command buffer for buffer copy to finish")
            vkFreeCommandBuffers(logicalDevice.handle, allocateInfo.commandPool(), commandBuffer)
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
