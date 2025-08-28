package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.toByteBuffer
import dev.cryptospace.anvil.core.scene.GameObject
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.vulkan.VulkanTexture
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.pipeline.Pipeline
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.VkDescriptorSet
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
import org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.util.EnumSet
import kotlin.reflect.KClass

class VulkanDrawLoop(
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
    private val textureManager: TextureManager,
    private val pipelineTextured3D: Pipeline,
    private val materialDescriptorSet: VkDescriptorSet,
    private val maxMaterialCount: Int,
) {

    private val vulkanMeshCache: Registry<VulkanMesh> = Registry()

    //    private val vulkanMaterialCache: Registry<VulkanMaterial> = Registry()
    private var materialCount: Int = 1

    fun <V : Vertex> addMesh(vertexType: KClass<V>, vertices: Array<V>, indices: Array<UInt>): MeshId {
        val verticesBytes = vertices.toByteBuffer()
        val indicesBytes = indices.toByteBuffer()

        val vertexBufferResource =
            bufferManager.allocateBuffer(
                verticesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX_BUFFER),
                EnumSet.of(BufferProperties.DEVICE_LOCAL),
            )
        val indexBufferResource =
            bufferManager.allocateBuffer(
                indicesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX_BUFFER),
                EnumSet.of(BufferProperties.DEVICE_LOCAL),
            )

        bufferManager.withStagingBuffer(verticesBytes) { stagingBuffer, fence ->
            bufferManager.transferBuffer(stagingBuffer, vertexBufferResource, fence)
        }
        bufferManager.withStagingBuffer(indicesBytes) { stagingBuffer, fence ->
            bufferManager.transferBuffer(stagingBuffer, indexBufferResource, fence)
        }

        val graphicsPipeline = when (vertexType) {
            TexturedVertex3::class -> pipelineTextured3D
            else -> error("Unsupported vertex type: $vertexType")
        }

        val mesh = VulkanMesh(
            vertexBufferAllocation = vertexBufferResource,
            indexBufferAllocation = indexBufferResource,
            indexCount = indices.size,
            indexType = VK_INDEX_TYPE_UINT32,
            pipeline = graphicsPipeline,
        )
        return MeshId(vulkanMeshCache.add(mesh))
    }

    fun addMaterial(texture: VulkanTexture): MaterialId = MemoryStack.stackPush().use { stack ->
        check(materialCount < maxMaterialCount) { "Too many materials" }
        val textureIndex = materialCount++
        val materialId = MaterialId(textureIndex)

        val imageInfo = VkDescriptorImageInfo.calloc(stack)
            .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            .imageView(texture.textureImageView.handle.value)
            .sampler(textureManager.sampler.value)
        val imageInfos = VkDescriptorImageInfo.calloc(1, stack)
            .put(imageInfo)
            .flip()
        val imageInfoDescriptorWrite = VkWriteDescriptorSet.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(materialDescriptorSet.value)
            .dstBinding(0)
            .dstArrayElement(textureIndex)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .descriptorCount(1)
            .pImageInfo(imageInfos)
        val writeDescriptorSet = VkWriteDescriptorSet.calloc(1, stack)
            .put(imageInfoDescriptorWrite)
            .flip()

        vkUpdateDescriptorSets(logicalDevice.handle, writeDescriptorSet, null)

        return materialId
    }

    fun draw(stack: MemoryStack, commandBuffer: CommandBuffer, gameObject: GameObject) {
        val renderComponent = gameObject.renderComponent ?: return
        val meshId = renderComponent.meshId ?: return
        val mesh = vulkanMeshCache[meshId.value] ?: return

        val vertexBuffers = stack.longs(mesh.vertexBufferAllocation.buffer.value)
        val offsets = stack.longs(0L)

        VK10.vkCmdPushConstants(
            commandBuffer.handle,
            mesh.pipeline.pipelineLayoutHandle.value,
            VK10.VK_SHADER_STAGE_VERTEX_BIT,
            0,
            gameObject.transform.toByteBuffer(stack),
        )
        VK10.vkCmdPushConstants(
            commandBuffer.handle,
            mesh.pipeline.pipelineLayoutHandle.value,
            VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
            Mat4.BYTE_SIZE,
            stack.ints(renderComponent.materialId?.value ?: 0),
        )
        VK10.vkCmdBindVertexBuffers(commandBuffer.handle, 0, vertexBuffers, offsets)
        VK10.vkCmdBindIndexBuffer(
            commandBuffer.handle,
            mesh.indexBufferAllocation.buffer.value,
            0L,
            mesh.indexType,
        )

        VK10.vkCmdDrawIndexed(commandBuffer.handle, mesh.indexCount, 1, 0, 0, 0)
    }
}
