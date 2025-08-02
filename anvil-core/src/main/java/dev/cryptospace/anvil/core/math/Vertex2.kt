package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class Vertex2(
    val position: Vec2,
    val color: Vec3,
    val textureCoordinates: Vec2 = Vec2.zero,
) : NativeType {

    override val byteSize: Int = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        position.toByteBuffer(byteBuffer)
        color.toByteBuffer(byteBuffer)
        textureCoordinates.toByteBuffer(byteBuffer)
    }

    companion object {

        const val BYTE_SIZE = Vec2.BYTE_SIZE + Vec3.BYTE_SIZE + Vec2.BYTE_SIZE
    }
}
