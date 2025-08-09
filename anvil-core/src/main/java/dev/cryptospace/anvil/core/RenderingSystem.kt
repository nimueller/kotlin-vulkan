package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.Texture
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import java.nio.ByteBuffer
import kotlin.reflect.KClass

abstract class RenderingSystem : NativeResource() {

    abstract fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4

    abstract fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Texture

    abstract fun <V : Vertex> uploadMesh(vertexType: KClass<V>, vertices: Array<V>, indices: Array<UShort>): Mesh

    abstract fun <V : Vertex> uploadMesh(vertexType: KClass<V>, vertices: Array<V>, indices: Array<UInt>): Mesh

    abstract fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit)
}
