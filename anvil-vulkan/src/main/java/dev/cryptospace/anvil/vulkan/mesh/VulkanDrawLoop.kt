package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.collections.Registry
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.math.toByteBuffer
import dev.cryptospace.anvil.core.scene.HUD
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MaterialProperties
import dev.cryptospace.anvil.core.scene.MaterialRegistry
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.Scene
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.vulkan.VulkanTexture
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.pipeline.Pipeline
import dev.cryptospace.anvil.vulkan.pipeline.PipelineManager
import dev.cryptospace.anvil.vulkan.pipeline.PushConstants
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.VkDescriptorSet
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
import org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.nio.ByteBuffer
import java.util.EnumSet

private val DEFAULT_MATERIAL_PROPERTIES = MaterialProperties()

/**
 * Owns the GPU meshes and texture slots and records the draw commands for the scene and the HUD.
 */
class VulkanDrawLoop(
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
    private val textureManager: TextureManager,
    private val pipelineManager: PipelineManager,
    private val materials: MaterialRegistry,
    private val textureDescriptorSet: VkDescriptorSet,
    private val maxTextureCount: Int,
) {

    private val meshes: Registry<VulkanMesh> = Registry()

    /** Slot 0 is reserved for [PushConstants.NO_TEXTURE_INDEX]. */
    private var textureCount: Int = PushConstants.NO_TEXTURE_INDEX + 1

    private class DrawItem(
        val mesh: VulkanMesh,
        val material: Material?,
        val transform: Mat4,
    )

    fun <V : Vertex> addMesh(vertexLayout: VertexLayout<V>, vertices: Array<V>, indices: Array<UInt>): MeshId {
        require(vertices.isNotEmpty()) { "Mesh must have at least one vertex" }
        require(indices.isNotEmpty()) { "Mesh must have at least one index" }
        require(indices.all { index -> index < vertices.size.toUInt() }) {
            "Mesh indices must be smaller than the vertex count ${vertices.size}"
        }

        val vertexBufferResource = uploadDeviceLocal(vertices.toByteBuffer(), BufferUsage.VERTEX_BUFFER)
        val indexBufferResource = uploadDeviceLocal(indices.toByteBuffer(), BufferUsage.INDEX_BUFFER)

        val mesh = VulkanMesh(
            vertexLayout = vertexLayout,
            vertexBufferAllocation = vertexBufferResource,
            indexBufferAllocation = indexBufferResource,
            indexCount = indices.size,
            indexType = VK_INDEX_TYPE_UINT32,
        )
        return MeshId(meshes.add(mesh))
    }

    /**
     * Copies [bytes] into a new device-local buffer and frees [bytes].
     */
    private fun uploadDeviceLocal(bytes: ByteBuffer, usage: BufferUsage) = try {
        bufferManager.allocateBuffer(
            bytes.remaining().toLong(),
            EnumSet.of(BufferUsage.TRANSFER_DST, usage),
            EnumSet.of(BufferProperties.DEVICE_LOCAL),
        ).also { allocation ->
            bufferManager.withStagingBuffer(bytes) { stagingBuffer, fence ->
                bufferManager.transferBuffer(stagingBuffer, allocation, fence)
            }
        }
    } finally {
        MemoryUtil.memFree(bytes)
    }

    /**
     * Unregisters a mesh so it is no longer drawn and returns it.
     * The caller is responsible for freeing its buffers once the GPU is done with them.
     */
    fun removeMesh(meshId: MeshId): VulkanMesh =
        meshes.remove(meshId.value) ?: throw IllegalArgumentException("Unknown mesh $meshId")

    fun freeMesh(mesh: VulkanMesh) {
        bufferManager.freeBuffer(mesh.vertexBufferAllocation)
        bufferManager.freeBuffer(mesh.indexBufferAllocation)
    }

    fun isValidTexture(textureId: TextureId): Boolean =
        textureId.value > PushConstants.NO_TEXTURE_INDEX && textureId.value < textureCount

    fun addTexture(texture: VulkanTexture): TextureId = MemoryStack.stackPush().use { stack ->
        check(textureCount < maxTextureCount) { "Too many textures, at most ${maxTextureCount - 1} are supported" }
        val textureIndex = textureCount++
        val textureId = TextureId(textureIndex)

        val imageInfo = VkDescriptorImageInfo.calloc(stack)
            .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            .imageView(texture.textureImageView.handle.value)
            .sampler(textureManager.sampler.value)
        val imageInfos = VkDescriptorImageInfo.calloc(1, stack)
            .put(imageInfo)
            .flip()
        val imageInfoDescriptorWrite = VkWriteDescriptorSet.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(textureDescriptorSet.value)
            .dstBinding(0)
            .dstArrayElement(textureIndex)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .descriptorCount(1)
            .pImageInfo(imageInfos)
        val writeDescriptorSet = VkWriteDescriptorSet.calloc(1, stack)
            .put(imageInfoDescriptorWrite)
            .flip()

        vkUpdateDescriptorSets(logicalDevice.handle, writeDescriptorSet, null)

        return textureId
    }

    /**
     * Draws all visible game objects. Opaque materials are drawn first, then blended ones,
     * so transparent surfaces blend over everything behind them.
     */
    fun drawScene(stack: MemoryStack, commandBuffer: CommandBuffer, frameDescriptorSet: VkDescriptorSet, scene: Scene) {
        // TODO introduce a scene dirty flag and cache this grouping, instead of recreating it every frame.
        val items = scene.gameObjects.mapNotNull { gameObject ->
            if (!gameObject.visible) return@mapNotNull null
            val renderComponent = gameObject.renderComponent ?: return@mapNotNull null
            val mesh = renderComponent.meshId?.let { meshId -> meshes[meshId.value] } ?: return@mapNotNull null
            DrawItem(mesh, resolveMaterial(renderComponent.materialId), gameObject.transform)
        }
        val (transparentItems, opaqueItems) = items.partition { item ->
            item.material?.blendMode?.isTransparent == true
        }

        drawGroupedByPipeline(stack, commandBuffer, frameDescriptorSet, opaqueItems)
        drawGroupedByPipeline(stack, commandBuffer, frameDescriptorSet, transparentItems)
    }

    private fun drawGroupedByPipeline(
        stack: MemoryStack,
        commandBuffer: CommandBuffer,
        frameDescriptorSet: VkDescriptorSet,
        items: List<DrawItem>,
    ) {
        val itemsByPipeline = items.groupBy { item ->
            pipelineManager.getPipeline(item.mesh.vertexLayout, item.material)
        }

        for ((pipeline, pipelineItems) in itemsByPipeline) {
            pipeline.bind(stack, commandBuffer, listOf(frameDescriptorSet, textureDescriptorSet))
            pipelineItems.forEach { item -> drawItem(stack, commandBuffer, pipeline, item) }
        }
    }

    /**
     * Draws the HUD labels on top of the scene.
     *
     * @param width Width of the render target in pixels
     * @param height Height of the render target in pixels
     */
    fun drawHUD(
        stack: MemoryStack,
        commandBuffer: CommandBuffer,
        frameDescriptorSet: VkDescriptorSet,
        hud: HUD,
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) {
            return
        }

        // Maps pixels (origin top-left, Y down) to Vulkan clip space, which also has Y pointing down
        val pixelsToClipSpace = Mat4.translation(Vec3(-1f, -1f, 0f)) *
            Mat4.scaling(Vec3(2f / width, 2f / height, 1f))
        var boundPipeline: Pipeline? = null

        for (label in hud.labels) {
            val mesh = label.meshId?.let { meshId -> meshes[meshId.value] } ?: continue
            val material = resolveMaterial(label.materialId)
            val pipeline = pipelineManager.getPipeline(mesh.vertexLayout, material)

            if (pipeline !== boundPipeline) {
                pipeline.bind(stack, commandBuffer, listOf(frameDescriptorSet, textureDescriptorSet))
                boundPipeline = pipeline
            }

            val transform = pixelsToClipSpace * Mat4.translation(Vec3(label.position.x, label.position.y, 0f))
            drawItem(stack, commandBuffer, pipeline, DrawItem(mesh, material, transform))
        }
    }

    private fun resolveMaterial(materialId: MaterialId?): Material? = materialId?.let(materials::resolveOrNull)

    private fun drawItem(stack: MemoryStack, commandBuffer: CommandBuffer, pipeline: Pipeline, item: DrawItem) =
        stack.push().use { drawStack ->
            val mesh = item.mesh

            VK10.vkCmdPushConstants(
                commandBuffer.handle,
                pipeline.pipelineLayoutHandle.value,
                VK10.VK_SHADER_STAGE_VERTEX_BIT,
                PushConstants.VERTEX_OFFSET,
                item.transform.toByteBuffer(drawStack),
            )

            val fragmentPushConstants = drawStack.malloc(PushConstants.FRAGMENT_SIZE)
            PushConstants.writeFragment(
                fragmentPushConstants,
                item.material?.properties ?: DEFAULT_MATERIAL_PROPERTIES,
                item.material?.texture?.value ?: PushConstants.NO_TEXTURE_INDEX,
            )
            fragmentPushConstants.flip()

            VK10.vkCmdPushConstants(
                commandBuffer.handle,
                pipeline.pipelineLayoutHandle.value,
                VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                PushConstants.FRAGMENT_OFFSET,
                fragmentPushConstants,
            )
            VK10.vkCmdBindVertexBuffers(
                commandBuffer.handle,
                0,
                drawStack.longs(mesh.vertexBufferAllocation.buffer.value),
                drawStack.longs(0L),
            )
            VK10.vkCmdBindIndexBuffer(
                commandBuffer.handle,
                mesh.indexBufferAllocation.buffer.value,
                0L,
                mesh.indexType,
            )

            VK10.vkCmdDrawIndexed(commandBuffer.handle, mesh.indexCount, 1, 0, 0, 0)
        }
}
