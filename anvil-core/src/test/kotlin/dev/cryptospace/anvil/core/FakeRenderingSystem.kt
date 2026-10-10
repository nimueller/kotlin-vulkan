package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.ImageManager
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.MaterialRegistry
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * In-memory rendering system for testing engine logic without a GPU.
 */
class FakeRenderingSystem : RenderingSystem() {

    class UploadedMesh(
        val vertexLayout: VertexLayout<*>,
        val vertexCount: Int,
        val vertexData: ByteBuffer,
        val indices: IntArray,
    )

    val meshes = mutableMapOf<MeshId, UploadedMesh>()
    val removedMeshes = mutableListOf<MeshId>()
    val shaders = mutableListOf<ShaderType>()
    var textureCount = 0
        private set
    private var nextMeshId = 0

    val materialRegistry: MaterialRegistry
        get() = materials

    override val imageManager: ImageManager = ImageManager(this)

    override fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4 =
        Mat4.perspectiveVulkan(fov, aspect, near, far)

    override fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId {
        shaders += shaderType
        return ShaderId(shaders.size - 1L)
    }

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): TextureId =
        TextureId(++textureCount)

    override fun uploadMesh(
        vertexLayout: VertexLayout<*>,
        vertexCount: Int,
        indices: IntArray,
        writeVertices: (ByteBuffer) -> Unit,
    ): MeshId {
        val vertexData = ByteBuffer.allocate(vertexCount * vertexLayout.byteSize).order(ByteOrder.nativeOrder())
        writeVertices(vertexData)

        val meshId = MeshId(nextMeshId++)
        meshes[meshId] = UploadedMesh(vertexLayout, vertexCount, vertexData, indices.copyOf())
        return meshId
    }

    override fun removeMesh(meshId: MeshId) {
        requireNotNull(meshes.remove(meshId)) { "Unknown mesh $meshId" }
        removedMeshes += meshId
    }

    override fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit) = Unit

    override fun shaderTypeOf(shaderId: ShaderId): ShaderType =
        shaders.getOrNull(shaderId.value.toInt()) ?: throw IllegalArgumentException("Unknown shader $shaderId")

    override fun destroy() = Unit
}
