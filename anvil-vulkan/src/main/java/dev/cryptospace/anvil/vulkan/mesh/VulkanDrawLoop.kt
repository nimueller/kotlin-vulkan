package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.toByteBuffer
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.scene.GameObject
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.vulkan.VulkanMesh
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import java.util.*
import kotlin.reflect.KClass

class VulkanDrawLoop(
    private val bufferManager: BufferManager,
    private val graphicsPipelineTextured3D: GraphicsPipeline,
) {

    private val vulkanMeshCache: Registry<VulkanMesh> = Registry()
    private val vulkanMaterialCache: Registry<VulkanMaterial> = Registry()

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
            TexturedVertex3::class -> graphicsPipelineTextured3D
            else -> error("Unsupported vertex type: $vertexType")
        }

        val meshReference = Mesh(visible = true, modelMatrix = Mat4.identity)
        val mesh = VulkanMesh(
            mesh = meshReference,
            vertexBufferAllocation = vertexBufferResource,
            indexBufferAllocation = indexBufferResource,
            indexCount = indices.size,
            indexType = VK_INDEX_TYPE_UINT32,
            graphicsPipeline = graphicsPipeline,
        )
        return MeshId(vulkanMeshCache.add(mesh))
    }

    fun addMaterial(material: Material): MaterialId {
        val vulkanMaterial: VulkanMaterial = TODO("converting vertices and indices to Vulkan buffers")
        return MaterialId(vulkanMaterialCache.add(vulkanMaterial))
    }

    fun draw(stack: MemoryStack, commandBuffer: CommandBuffer, gameObject: GameObject) {
        val renderComponent = gameObject.renderComponent ?: return
        val meshId = renderComponent.meshId ?: return
//        val materialId = renderComponent.materialId ?: return

        val mesh = vulkanMeshCache[meshId.value] ?: return
//        val material = vulkanMaterialCache[materialId.value]

        val vertexBuffers = stack.longs(mesh.vertexBufferAllocation.buffer.value)
        val offsets = stack.longs(0L)

        vkCmdPushConstants(
            commandBuffer.handle,
            mesh.graphicsPipeline.pipelineLayoutHandle.value,
            VK_SHADER_STAGE_VERTEX_BIT,
            0,
            gameObject.transform.toByteBuffer(stack),
        )
        vkCmdBindVertexBuffers(commandBuffer.handle, 0, vertexBuffers, offsets)
        vkCmdBindIndexBuffer(
            commandBuffer.handle,
            mesh.indexBufferAllocation.buffer.value,
            0L,
            mesh.indexType,
        )

        vkCmdDrawIndexed(commandBuffer.handle, mesh.indexCount, 1, 0, 0, 0)
    }
}
