package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class TexturedVertex2(
    val position: Vec2,
    val color: Vec3,
    val texture: Vec2 = Vec2.zero,
) : Vertex {

    override val vertexLayout: VertexLayout<TexturedVertex2>
        get() = Companion

    companion object : VertexLayout<TexturedVertex2>() {

        val position = float2()
        val color = float3()
        val texture = float2()

        override fun write(vertex: TexturedVertex2, buffer: ByteBuffer, offset: Int) {
            position.put(buffer, offset, vertex.position)
            color.put(buffer, offset, vertex.color)
            texture.put(buffer, offset, vertex.texture)
        }
    }
}
