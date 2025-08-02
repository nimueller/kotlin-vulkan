package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.reflect.KProperty1

data class TexturedVertex3(
    val position: Vec3,
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

    companion object : VertexLayout<TexturedVertex3> {

        override val byteSize: Int = Vec3.BYTE_SIZE + Vec3.BYTE_SIZE + Vec2.BYTE_SIZE

        override fun offset(property: KProperty1<TexturedVertex3, *>): Int = when (property) {
            TexturedVertex3::position -> 0
            TexturedVertex3::color -> Vec3.BYTE_SIZE
            TexturedVertex3::texture -> Vec3.BYTE_SIZE + Vec3.BYTE_SIZE
            else -> throw IllegalArgumentException("Unknown property $property")
        }

        override fun getAttributeDescriptions(binding: Int): List<AttributeDescription> = listOf(
            AttributeDescription(binding, 0, AttributeFormat.SIGNED_FLOAT_3D, offset(TexturedVertex3::position)),
            AttributeDescription(binding, 1, AttributeFormat.SIGNED_FLOAT_3D, offset(TexturedVertex3::color)),
            AttributeDescription(binding, 2, AttributeFormat.SIGNED_FLOAT_2D, offset(TexturedVertex3::texture)),
        )
    }
}
