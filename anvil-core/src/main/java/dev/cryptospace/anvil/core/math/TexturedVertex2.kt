package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.reflect.KProperty1

data class TexturedVertex2(
    val position: Vec2,
    val color: Vec3,
    val texture: Vec2 = Vec2.zero,
) : NativeType,
    Vertex {

    override val byteSize: Int = Companion.byteSize

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        position.toByteBuffer(byteBuffer)
        color.toByteBuffer(byteBuffer)
        texture.toByteBuffer(byteBuffer)
    }

    companion object : VertexLayout<TexturedVertex2> {

        override val byteSize: Int = Vec2.BYTE_SIZE + Vec3.BYTE_SIZE + Vec2.BYTE_SIZE

        override fun offset(property: KProperty1<TexturedVertex2, *>): Int = when (property) {
            TexturedVertex2::position -> 0
            TexturedVertex2::color -> Vec2.BYTE_SIZE
            TexturedVertex2::texture -> Vec2.BYTE_SIZE + Vec3.BYTE_SIZE
            else -> throw IllegalArgumentException("Unknown property $property")
        }

        override fun getAttributeDescriptions(binding: Int): List<AttributeDescription> = listOf(
            AttributeDescription(binding, 0, AttributeFormat.SIGNED_FLOAT_2D, offset(TexturedVertex2::position)),
            AttributeDescription(binding, 1, AttributeFormat.SIGNED_FLOAT_3D, offset(TexturedVertex2::color)),
            AttributeDescription(binding, 2, AttributeFormat.SIGNED_FLOAT_2D, offset(TexturedVertex2::texture)),
        )
    }
}
