package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import java.nio.ByteBuffer
import kotlin.reflect.KClass

abstract class RenderingSystem : NativeResource() {

    abstract fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Image

    abstract fun <V : Vertex> uploadMesh(vertexType: KClass<V>, vertices: List<V>, indices: List<Short>): Mesh

    abstract fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit)
}
