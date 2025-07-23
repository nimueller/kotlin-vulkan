package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
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

    private val buffers = mutableListOf<BufferResource>()

    /**
     * Creates a vertex buffer and uploads the provided vertices to GPU memory.
     *
     * @param vertices List of vertices to upload to the buffer
     * @return A BufferResource containing the buffer and its associated memory
     */
    fun createVertexBuffer(vertices: List<Vertex2>): BufferResource {
        val verticesBufferSize = vertices.size * Vertex2.SIZE.toLong()

        // Create the buffer
        val buffer = MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                size(verticesBufferSize)
                usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
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
                    findMemoryType(
                        bufferMemoryRequirements.memoryTypeBits(),
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    ),
                )
            }

            val pDeviceMemory = stack.mallocLong(1)
            vkAllocateMemory(logicalDevice.handle, memoryAllocateInfo, null, pDeviceMemory)
                .validateVulkanSuccess("Allocate buffer memory", "Failed to allocate memory for vertex buffer")
            vkBindBufferMemory(logicalDevice.handle, buffer.value, pDeviceMemory[0], 0)
                .validateVulkanSuccess("Bind buffer memory", "Failed to bind memory to vertex buffer")

            VkDeviceMemory(pDeviceMemory[0])
        }

        // Upload vertex data
        uploadVertexData(memory, vertices, verticesBufferSize)

        val bufferResource = BufferResource(logicalDevice, buffer, memory)
        buffers.add(bufferResource)

        logger.debug("Created vertex buffer: $bufferResource")
        return bufferResource
    }

    /**
     * Uploads vertex data to the specified device memory.
     *
     * @param memory The device memory to upload data to
     * @param vertices The vertices to upload
     * @param bufferSize The size of the buffer in bytes
     */
    private fun uploadVertexData(memory: VkDeviceMemory, vertices: List<Vertex2>, bufferSize: Long) {
        MemoryStack.stackPush().use { stack ->
            val verticesBuffer = stack.malloc(bufferSize.toInt())
            vertices.forEach { vertex ->
                verticesBuffer
                    .putFloat(vertex.position.x)
                    .putFloat(vertex.position.y)
                    .putFloat(vertex.color.x)
                    .putFloat(vertex.color.y)
                    .putFloat(vertex.color.z)
            }
            verticesBuffer.flip()
            val verticesBufferAddress = MemoryUtil.memAddress(verticesBuffer)

            val pMemory = stack.mallocPointer(1)
            vkMapMemory(logicalDevice.handle, memory.value, 0, bufferSize, 0, pMemory)
                .validateVulkanSuccess("Map vertex buffer memory", "Failed to map memory for uploading vertex data")
            MemoryUtil.memCopy(verticesBufferAddress, pMemory[0], bufferSize)
            vkUnmapMemory(logicalDevice.handle, memory.value)
        }
    }

    /**
     * Finds a memory type that satisfies the given requirements.
     *
     * @param typeFilter Bit field of memory types that are suitable
     * @param properties Required memory properties
     * @return Index of a suitable memory type
     * @throws IllegalStateException if no suitable memory type is found
     */
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

        throw IllegalStateException("Failed to find suitable memory type")
    }

    /**
     * Destroys all buffer resources managed by this BufferManager.
     */
    override fun destroy() {
        buffers.forEach { buffer: BufferResource -> buffer.close() }
        buffers.clear()
    }

    companion object {

        @JvmStatic
        private val logger = logger<BufferManager>()
    }
}
