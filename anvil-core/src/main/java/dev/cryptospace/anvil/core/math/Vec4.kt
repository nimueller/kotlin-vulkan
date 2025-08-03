package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

data class Vec4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) : NativeType {

    constructor(vec3: Vec3, w: Float) : this(
        vec3.x,
        vec3.y,
        vec3.z,
        w,
    )

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        byteBuffer.putFloat(x)
        byteBuffer.putFloat(y)
        byteBuffer.putFloat(z)
        byteBuffer.putFloat(w)
    }

    infix fun dot(other: Vec4): Float = x * other.x + y * other.y + z * other.z + w * other.w

    operator fun times(scalar: Float): Vec4 = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)

    operator fun div(scalar: Float): Vec4 = Vec4(x / scalar, y / scalar, z / scalar, w / scalar)

    operator fun plus(other: Vec4): Vec4 = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)

    operator fun minus(other: Vec4): Vec4 = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)

    operator fun unaryMinus(): Vec4 = Vec4(-x, -y, -z, -w)

    operator fun get(index: Int): Float = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IndexOutOfBoundsException("index must be in range [0, 3]")
    }

    companion object {

        const val BYTE_SIZE = 4 * Float.SIZE_BYTES

        val zero = Vec4(0f, 0f, 0f, 0f)
        val one = Vec4(1f, 1f, 1f, 1f)
        val right = Vec4(1f, 0f, 0f, 0f)
        val left = Vec4(-1f, 0f, 0f, 0f)
        val up = Vec4(0f, 1f, 0f, 0f)
        val down = Vec4(0f, -1f, 0f, 0f)
        val forward = Vec4(0f, 0f, 1f, 0f)
        val backward = Vec4(0f, 0f, -1f, 0f)
    }
}
