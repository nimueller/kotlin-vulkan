package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.native.UniformBufferObject

interface RenderingContext {

    val width: Int

    val height: Int

    var uniformBufferObject: UniformBufferObject

    fun drawMesh(mesh: Mesh)
}
