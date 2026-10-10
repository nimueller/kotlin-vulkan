package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class TexturedVertex3(
    val position: Vec3,
    val color: Vec3,
    val texture: Vec2 = Vec2.zero,
) : Vertex {

    override val vertexLayout: VertexLayout<TexturedVertex3>
        get() = Companion

    companion object : VertexLayout<TexturedVertex3>() {

        val position = float3()
        val color = float3()
        val texture = float2()

        override fun write(vertex: TexturedVertex3, buffer: ByteBuffer, offset: Int) {
            position.put(buffer, offset, vertex.position)
            color.put(buffer, offset, vertex.color)
            texture.put(buffer, offset, vertex.texture)
        }
    }
}
