package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum.Companion.toBitmask
import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.context.VulkanContext
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.image.Image
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
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
    private val context: VulkanContext,
    private val logicalDevice: LogicalDevice,
    private val commandPool: CommandPool,
) : NativeResource() {

    private val memoryAllocatorHandle: VmaAllocator =
        MemoryStack.stackPush().use { stack ->
            val functions = VmaVulkanFunctions.calloc(stack).apply {
                set(context.handle, logicalDevice.handle)
            }

            val createInfo = VmaAllocatorCreateInfo.calloc(stack).apply {
                flags(0)
                physicalDevice(logicalDevice.physicalDevice.handle)
                device(logicalDevice.handle)
                instance(context.handle)
                vulkanApiVersion(VK10.VK_API_VERSION_1_0)
                pVulkanFunctions(functions)
                pAllocationCallbacks(null)
            }

            val pAllocator = stack.mallocPointer(1)
            Vma.vmaCreateAllocator(createInfo, pAllocator)
            VmaAllocator(pAllocator[0])
        }

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

    fun allocateImageBuffer(image: Image): VkDeviceMemory = MemoryStack.stackPush().use { stack ->
        val bufferMemoryRequirements = VkMemoryRequirements.calloc(stack)
        vkGetImageMemoryRequirements(logicalDevice.handle, image.handle.value, bufferMemoryRequirements)

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
        vkBindImageMemory(logicalDevice.handle, image.handle.value, pTextureImageMemory[0], 0)
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

        recordSingleTimeCommands { stack, commandBuffer ->
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

    fun transitionImageLayout(image: Image, format: Int, oldLayout: Int, newLayout: Int) {
        recordSingleTimeCommands { stack, commandBuffer ->
            var sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            var destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT

            val barrier = VkImageMemoryBarrier.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                oldLayout(oldLayout)
                newLayout(newLayout)
                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                image(image.handle.value)
                subresourceRange { range ->
                    range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    range.baseMipLevel(0)
                    range.levelCount(1)
                    range.baseArrayLayer(0)
                    range.layerCount(1)
                }

                if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    srcAccessMask(0)
                    dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                    destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
                    newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                ) {
                    srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                    destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                } else {
                    error("Unsupported layout transition")
                }
            }

            val barriers = VkImageMemoryBarrier.calloc(1, stack)
                .put(barrier)
                .flip()

            vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barriers)
        }
    }

    fun copyBufferToImage(buffer: VkBuffer, image: Image, width: Int, height: Int) {
        recordSingleTimeCommands { stack, commandBuffer ->
            val region = VkBufferImageCopy.calloc(stack).apply {
                bufferOffset(0)
                bufferRowLength(0)
                bufferImageHeight(0)
                imageSubresource { subresource ->
                    subresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    subresource.mipLevel(0)
                    subresource.baseArrayLayer(0)
                    subresource.layerCount(1)
                }
                imageOffset { offset ->
                    offset.x(0)
                    offset.y(0)
                    offset.z(0)
                }
                imageExtent { extent ->
                    extent.width(width)
                    extent.height(height)
                    extent.depth(1)
                }
            }

            val regions = VkBufferImageCopy.calloc(1, stack)
                .put(region)
                .flip()

            vkCmdCopyBufferToImage(
                commandBuffer,
                buffer.value,
                image.handle.value,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                regions,
            )
        }
    }

    private fun recordSingleTimeCommands(callback: (MemoryStack, VkCommandBuffer) -> Unit) =
        MemoryStack.stackPush().use { stack ->
            val commandBuffer = beginSingleTimeCommands(stack)
            callback(stack, commandBuffer)
            endSingleTimeCommands(stack, commandBuffer)
        }

    private fun beginSingleTimeCommands(stack: MemoryStack): VkCommandBuffer {
        val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandBufferCount(1)
            commandPool(commandPool.handle.value)
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
        return commandBuffer
    }

    private fun endSingleTimeCommands(stack: MemoryStack, commandBuffer: VkCommandBuffer) {
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
        vkFreeCommandBuffers(logicalDevice.handle, commandPool.handle.value, commandBuffer)
    }

    /**
     * Destroys all buffer resources managed by this BufferManager.
     */
    override fun destroy() {
        buffers.forEach { buffer -> buffer.close() }
        buffers.clear()

        Vma.vmaDestroyAllocator(memoryAllocatorHandle.value)
    }

    companion object {

        @JvmStatic
        private val logger = logger<BufferManager>()
    }
}
