package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class Vec4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) : NativeBuffer {

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        byteBuffer.putFloat(x)
        byteBuffer.putFloat(y)
        byteBuffer.putFloat(z)
        byteBuffer.putFloat(w)
    }

    operator fun times(scalar: Float): Vec4 = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)

    operator fun times(other: Vec4): Vec4 = Vec4(x * other.x, y * other.y, z * other.z, w * other.w)

    operator fun div(scalar: Float): Vec4 = Vec4(x / scalar, y / scalar, z / scalar, w / scalar)

    operator fun plus(other: Vec4): Vec4 = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)

    operator fun minus(other: Vec4): Vec4 = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)

    operator fun unaryMinus(): Vec4 = Vec4(-x, -y, -z, -w)

    companion object {

        const val BYTE_SIZE = 4 * Float.SIZE_BYTES
    }
}
