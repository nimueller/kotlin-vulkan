package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.BitmaskEnum.Companion.toBitmask
import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.vkAllocateMemory
import org.lwjgl.vulkan.VK10.vkBindBufferMemory
import org.lwjgl.vulkan.VK10.vkCreateBuffer
import org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VK10.vkMapMemory
import org.lwjgl.vulkan.VK10.vkUnmapMemory
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
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

        val allocation = BufferAllocation(logicalDevice, buffer, memory)
        buffers.add(allocation)
        logger.debug {
            "Allocated buffer: $buffer with memory: $memory (size: $size) for usage: $usage and properties: $properties"
        }
        return allocation
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
     * Creates a vertex buffer and uploads the provided vertices to GPU memory.
     *
     * @param vertices List of vertices to upload to the buffer
     * @return A BufferResource containing the buffer and its associated memory
     */
    fun uploadVertexData(allocation: BufferAllocation, vertices: List<Vertex2>) {
        MemoryStack.stackPush().use { stack ->
            val verticesBuffer = stack.malloc(vertices.size * Vertex2.BYTE_SIZE)
            vertices.forEach { vertex ->
                verticesBuffer
                    .putFloat(vertex.position.x)
                    .putFloat(vertex.position.y)
                    .putFloat(vertex.color.x)
                    .putFloat(vertex.color.y)
                    .putFloat(vertex.color.z)
            }
            verticesBuffer.flip()

            uploadData(allocation, verticesBuffer)
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
        MemoryStack.stackPush().use { stack ->
            val verticesBufferAddress = MemoryUtil.memAddress(data)
            val size = data.remaining().toLong()

            val pMemory = stack.mallocPointer(1)
            vkMapMemory(logicalDevice.handle, allocation.memory.value, 0, size, 0, pMemory)
                .validateVulkanSuccess("Map vertex buffer memory", "Failed to map memory for uploading vertex data")
            MemoryUtil.memCopy(verticesBufferAddress, pMemory[0], size)
            vkUnmapMemory(logicalDevice.handle, allocation.memory.value)
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
