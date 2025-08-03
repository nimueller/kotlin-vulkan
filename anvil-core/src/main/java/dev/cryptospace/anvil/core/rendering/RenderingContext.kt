package dev.cryptospace.anvil.core.rendering

interface RenderingContext {

    val camera: Camera

    val width: Int

    val height: Int

    fun drawMesh(mesh: Mesh)
}
