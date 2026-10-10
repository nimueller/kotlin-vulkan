package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.collections.Registry
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.MatrixSource
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.scene.HUD
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MaterialProperties
import dev.cryptospace.anvil.core.scene.MaterialRegistry
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.Scene
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.vulkan.VulkanTexture
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.pipeline.InstanceData
import dev.cryptospace.anvil.vulkan.pipeline.MaterialPushConstants
import dev.cryptospace.anvil.vulkan.pipeline.PipelineManager
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.EnumSet

private val DEFAULT_MATERIAL_PROPERTIES = MaterialProperties()

/** Material slot used in sort keys for objects without a material. Real materials use their id + 1. */
private const val DEFAULT_MATERIAL_SLOT = 0

/**
 * Owns the GPU meshes and texture slots and records the draw commands for the scene and the HUD.
 *
 * Drawing goes through a [RenderQueue] sorted by pipeline, material and mesh. Consecutive objects sharing all three
 * are drawn with a single instanced draw call, reading their model matrices from the frame's instance buffer, and
 * each kind of GPU state is only changed when it differs from the previous batch. Steady-state frames allocate
 * nothing here.
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

    /** Slot 0 is reserved for [MaterialPushConstants.NO_TEXTURE_INDEX]. */
    private var textureCount: Int = MaterialPushConstants.NO_TEXTURE_INDEX + 1

    private val queue = RenderQueue<VulkanMesh>()

    private var instanceData: ByteBuffer? = null
    private var instanceCapacity = 0
    private var instanceCount = 0

    // Pipeline index per material id, valid while the material's version and the mesh's vertex layout are unchanged
    private var cachedPipelineVersions = IntArray(INITIAL_MATERIAL_CACHE_SIZE) { -1 }
    private var cachedPipelineLayouts = arrayOfNulls<VertexLayout<*>>(INITIAL_MATERIAL_CACHE_SIZE)
    private var cachedPipelineIndices = IntArray(INITIAL_MATERIAL_CACHE_SIZE)
    private val defaultMaterialPipelines = HashMap<VertexLayout<*>, Int>()

    fun addMesh(
        vertexLayout: VertexLayout<*>,
        vertexCount: Int,
        indices: IntArray,
        writeVertices: (ByteBuffer) -> Unit,
    ): MeshId {
        require(vertexCount > 0) { "Mesh must have at least one vertex" }
        require(indices.isNotEmpty()) { "Mesh must have at least one index" }
        for (index in indices) {
            require(Integer.compareUnsigned(index, vertexCount) < 0) {
                "Mesh index ${Integer.toUnsignedString(index)} is out of range for $vertexCount vertices"
            }
        }

        val vertexBytes = Math.multiplyExact(vertexCount, vertexLayout.byteSize)
        val indexBytes = Math.multiplyExact(indices.size, Int.SIZE_BYTES)
        val vertexBuffer = allocateDeviceLocal(vertexBytes, BufferUsage.VERTEX_BUFFER)
        val indexBuffer = try {
            allocateDeviceLocal(indexBytes, BufferUsage.INDEX_BUFFER)
        } catch (exception: Exception) {
            bufferManager.freeBuffer(vertexBuffer)
            throw exception
        }

        try {
            // One staging buffer and one submission for both buffers, written in place without intermediate copies
            bufferManager.withStagingBuffer(
                size = (vertexBytes + indexBytes).toLong(),
                fill = { staging ->
                    writeVertices(staging.slice(0, vertexBytes).order(ByteOrder.nativeOrder()))
                    staging.slice(vertexBytes, indexBytes).order(ByteOrder.nativeOrder()).asIntBuffer().put(indices)
                },
            ) { staging, fence ->
                bufferManager.copyBuffer(
                    staging,
                    listOf(
                        BufferManager.CopyRegion(0, vertexBuffer, vertexBytes.toLong()),
                        BufferManager.CopyRegion(vertexBytes.toLong(), indexBuffer, indexBytes.toLong()),
                    ),
                    fence,
                )
            }
        } catch (exception: Exception) {
            bufferManager.freeBuffer(vertexBuffer)
            bufferManager.freeBuffer(indexBuffer)
            throw exception
        }

        val mesh = VulkanMesh(
            vertexLayout = vertexLayout,
            vertexBufferAllocation = vertexBuffer,
            indexBufferAllocation = indexBuffer,
            indexCount = indices.size,
            indexType = VK_INDEX_TYPE_UINT32,
        )
        return MeshId(meshes.add(mesh))
    }

    private fun allocateDeviceLocal(size: Int, usage: BufferUsage): BufferAllocation = bufferManager.allocateBuffer(
        size.toLong(),
        EnumSet.of(BufferUsage.TRANSFER_DST, usage),
        EnumSet.of(BufferProperties.DEVICE_LOCAL),
    )

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
        textureId.value > MaterialPushConstants.NO_TEXTURE_INDEX && textureId.value < textureCount

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
     * Starts recording a frame. All draws of the frame write their model matrices to [instanceData],
     * which must have room for [capacity] instances and stay untouched by the GPU until the frame is submitted.
     */
    fun beginFrame(instanceData: ByteBuffer, capacity: Int) {
        this.instanceData = instanceData
        instanceCapacity = capacity
        instanceCount = 0
    }

    /**
     * Draws all visible game objects, opaque ones first, then blended ones.
     */
    fun drawScene(stack: MemoryStack, commandBuffer: CommandBuffer, frameDescriptorSet: VkDescriptorSet, scene: Scene) {
        val gameObjects = scene.gameObjects
        for (index in gameObjects.indices) {
            val gameObject = gameObjects[index]
            if (!gameObject.visible) continue
            val renderComponent = gameObject.renderComponent ?: continue
            val meshId = renderComponent.meshId ?: continue
            val mesh = meshes[meshId.value] ?: continue

            enqueue(meshId, mesh, renderComponent.materialId, gameObject.transform)
        }

        flushQueue(stack, commandBuffer, frameDescriptorSet)
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

        val scaleX = 2f / width
        val scaleY = 2f / height
        val labels = hud.labels
        for (index in labels.indices) {
            val label = labels[index]
            val meshId = label.meshId ?: continue
            val mesh = meshes[meshId.value] ?: continue

            // Maps pixels (origin top-left, Y down) to Vulkan clip space, which also has Y pointing down
            val transform = Mat4(
                scaleX, 0f, 0f, label.position.x * scaleX - 1f,
                0f, scaleY, 0f, label.position.y * scaleY - 1f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            )
            enqueue(meshId, mesh, label.materialId, transform)
        }

        flushQueue(stack, commandBuffer, frameDescriptorSet)
    }

    private fun enqueue(meshId: MeshId, mesh: VulkanMesh, materialId: MaterialId?, transform: MatrixSource) {
        val material = materialId?.let { id -> materials.resolveOrNull(id) }

        if (materialId == null || material == null) {
            val pipeline = defaultMaterialPipelines.getOrPut(mesh.vertexLayout) {
                pipelineManager.getPipelineIndex(mesh.vertexLayout, null)
            }
            queue.add(false, pipeline, DEFAULT_MATERIAL_SLOT, meshId.value, mesh, null, transform)
        } else {
            val pipeline = cachedPipelineIndex(materialId, material, mesh.vertexLayout)
            val transparent = material.blendMode.isTransparent
            queue.add(transparent, pipeline, materialId.value + 1, meshId.value, mesh, material, transform)
        }
    }

    private fun cachedPipelineIndex(materialId: MaterialId, material: Material, vertexLayout: VertexLayout<*>): Int {
        val id = materialId.value
        ensureMaterialCacheCapacity(id)

        val version = materials.versionOf(materialId)
        if (cachedPipelineVersions[id] == version && cachedPipelineLayouts[id] === vertexLayout) {
            return cachedPipelineIndices[id]
        }

        val pipeline = pipelineManager.getPipelineIndex(vertexLayout, material)
        cachedPipelineVersions[id] = version
        cachedPipelineLayouts[id] = vertexLayout
        cachedPipelineIndices[id] = pipeline
        return pipeline
    }

    private fun ensureMaterialCacheCapacity(id: Int) {
        if (id < cachedPipelineVersions.size) return

        val previousSize = cachedPipelineVersions.size
        val capacity = maxOf(id + 1, previousSize * 2)
        cachedPipelineVersions = cachedPipelineVersions.copyOf(capacity).also { versions ->
            versions.fill(-1, previousSize, capacity)
        }
        cachedPipelineLayouts = cachedPipelineLayouts.copyOf(capacity)
        cachedPipelineIndices = cachedPipelineIndices.copyOf(capacity)
    }

    /**
     * Sorts and records all queued draws. Runs of draws sharing pipeline, material and mesh become one instanced
     * draw call, and state is only changed when it differs from the previous batch.
     */
    private fun flushQueue(stack: MemoryStack, commandBuffer: CommandBuffer, frameDescriptorSet: VkDescriptorSet) {
        val count = queue.size
        if (count == 0) return

        val instances = checkNotNull(instanceData) { "beginFrame must be called before drawing" }
        check(instanceCount + count <= instanceCapacity) {
            "Instance buffer holds $instanceCapacity instances, but ${instanceCount + count} were drawn"
        }
        queue.sort()

        val handle = commandBuffer.handle
        val pipelineLayout = pipelineManager.pipelineLayout.value
        val materialPushConstants = stack.malloc(MaterialPushConstants.size)
        val vertexBuffers = stack.mallocLong(1)
        val vertexBufferOffsets = stack.longs(0L)

        // All pipelines share one layout, so descriptor sets and push constants stay bound across pipeline switches
        VK10.vkCmdBindDescriptorSets(
            handle,
            VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
            pipelineLayout,
            0,
            stack.longs(frameDescriptorSet.value, textureDescriptorSet.value),
            null,
        )

        var boundPipeline = -1
        var boundMaterialSlot = -1
        var batchStart = 0

        while (batchStart < count) {
            val stateKey = queue.stateKey(batchStart)
            var batchEnd = batchStart + 1
            while (batchEnd < count && queue.stateKey(batchEnd) == stateKey) {
                batchEnd++
            }

            val pipeline = queue.pipeline(batchStart)
            if (pipeline != boundPipeline) {
                VK10.vkCmdBindPipeline(
                    handle,
                    VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipelineManager.pipeline(pipeline).handle.value,
                )
                boundPipeline = pipeline
            }

            val materialSlot = queue.materialSlot(batchStart)
            if (materialSlot != boundMaterialSlot) {
                val material = queue.material(batchStart)
                MaterialPushConstants.write(
                    materialPushConstants,
                    material?.properties ?: DEFAULT_MATERIAL_PROPERTIES,
                    material?.texture?.value ?: MaterialPushConstants.NO_TEXTURE_INDEX,
                )
                VK10.vkCmdPushConstants(
                    handle,
                    pipelineLayout,
                    VK10.VK_SHADER_STAGE_FRAGMENT_BIT,
                    MaterialPushConstants.startOffset,
                    materialPushConstants,
                )
                boundMaterialSlot = materialSlot
            }

            // Batches always differ in state from their predecessor, so a new batch with the same pipeline and
            // material must have a different mesh: binding unconditionally is never redundant.
            val mesh = queue.mesh(batchStart)
            vertexBuffers.put(0, mesh.vertexBufferAllocation.buffer.value)
            VK10.vkCmdBindVertexBuffers(handle, 0, vertexBuffers, vertexBufferOffsets)
            VK10.vkCmdBindIndexBuffer(handle, mesh.indexBufferAllocation.buffer.value, 0L, mesh.indexType)

            val firstInstance = instanceCount
            for (position in batchStart until batchEnd) {
                // Written directly, as the transform may be a cached Transform rather than a Mat4
                queue.transform(position).putColumnMajor(
                    instances,
                    instanceCount * InstanceData.size + InstanceData.model.bufferOffset,
                )
                instanceCount++
            }

            VK10.vkCmdDrawIndexed(handle, mesh.indexCount, batchEnd - batchStart, 0, 0, firstInstance)
            batchStart = batchEnd
        }

        queue.clear()
    }

    private companion object {
        const val INITIAL_MATERIAL_CACHE_SIZE = 64
    }
}
