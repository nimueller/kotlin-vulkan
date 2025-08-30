package dev.cryptospace.anvil.core.math

interface Vertex : NativeType {

    val vertexLayout: VertexLayout<out Vertex>

    override val byteSize: Int
        get() = vertexLayout.byteSize
}
