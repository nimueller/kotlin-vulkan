package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) : NativeBuffer {

    override val byteSize: Int = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        byteBuffer.putFloat(x)
        byteBuffer.putFloat(y)
        byteBuffer.putFloat(z)
    }

    companion object {

        const val BYTE_SIZE = 3 * Float.SIZE_BYTES
    }
}
