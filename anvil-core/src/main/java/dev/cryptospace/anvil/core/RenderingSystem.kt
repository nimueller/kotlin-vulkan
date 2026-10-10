package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.ImageManager
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MaterialRegistry
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import java.nio.ByteBuffer

abstract class RenderingSystem : NativeResource() {

    /**
     * All materials known to this rendering system. Backends read from it while drawing.
     */
    protected val materials: MaterialRegistry = MaterialRegistry()

    abstract val imageManager: ImageManager

    abstract fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4

    abstract fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId

    abstract fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): TextureId

    abstract fun <V : Vertex> uploadMesh(vertexType: VertexLayout<V>, vertices: Array<V>, indices: Array<UInt>): MeshId

    /**
     * Releases a mesh. Backends may defer freeing GPU memory until in-flight frames no longer use it,
     * but the [meshId] must not be used for drawing anymore after this call.
     */
    abstract fun removeMesh(meshId: MeshId)

    abstract fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit)

    fun uploadMaterial(material: Material): MaterialId {
        validateMaterial(material)
        return materials.add(material)
    }

    /**
     * Replaces the material registered under [materialId]. Takes effect from the next frame on.
     */
    fun updateMaterial(materialId: MaterialId, material: Material) {
        validateMaterial(material)
        materials.update(materialId, material)
    }

    /**
     * Returns a copy of the material registered under [materialId], e.g. to modify and pass to [updateMaterial].
     */
    fun getMaterial(materialId: MaterialId): Material = materials[materialId]

    fun removeMaterial(materialId: MaterialId) {
        materials.remove(materialId)
    }

    /**
     * Checks that a material only references resources this backend knows about.
     * Called before a material is stored, so invalid materials fail early instead of during drawing.
     *
     * @throws IllegalArgumentException if the material is invalid
     */
    protected open fun validateMaterial(material: Material) {
        material.shaders.forEach { (shaderType, shaderId) ->
            require(shaderTypeOf(shaderId) == shaderType) {
                "Shader $shaderId is registered as ${shaderTypeOf(shaderId)} but used as $shaderType"
            }
        }
    }

    /**
     * Returns the stage a shader was uploaded for.
     *
     * @throws IllegalArgumentException if the shader is unknown
     */
    protected abstract fun shaderTypeOf(shaderId: ShaderId): ShaderType
}
