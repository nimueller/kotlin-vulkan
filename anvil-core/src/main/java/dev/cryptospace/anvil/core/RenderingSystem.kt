package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext

abstract class RenderingSystem : NativeResource() {

    abstract fun uploadMesh(vertex2: List<Vertex2>, indices: List<Short>): Mesh

    abstract fun drawFrame(callback: (RenderingContext) -> Unit)
}
