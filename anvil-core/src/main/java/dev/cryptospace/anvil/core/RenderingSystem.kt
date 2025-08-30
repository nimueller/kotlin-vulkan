package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import java.nio.ByteBuffer

abstract class RenderingSystem : NativeResource() {

    abstract fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4

    abstract fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId

    abstract fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): TextureId

    abstract fun uploadMaterial(material: Material): MaterialId

    abstract fun <V : Vertex> uploadMesh(vertexType: VertexLayout<V>, vertices: Array<V>, indices: Array<UInt>): MeshId

    abstract fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit)
}
