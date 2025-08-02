package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class Vec2(
    val x: Float,
    val y: Float,
) : NativeType {

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        byteBuffer.putFloat(x)
        byteBuffer.putFloat(y)
    }

    companion object {

        const val BYTE_SIZE = 2 * Float.SIZE_BYTES
        val zero = Vec2(0f, 0f)
    }
}
