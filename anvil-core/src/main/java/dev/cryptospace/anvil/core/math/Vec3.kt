package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.math.sqrt

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) : NativeBuffer {

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        byteBuffer.putFloat(x)
        byteBuffer.putFloat(y)
        byteBuffer.putFloat(z)
    }

    fun normalized(): Vec3 = this / length()

    fun length(): Float = sqrt(x * x + y * y + z * z)

    infix fun cross(other: Vec3): Vec3 = Vec3(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x,
    )

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    operator fun div(scalar: Float): Vec3 = Vec3(x / scalar, y / scalar, z / scalar)

    operator fun times(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    companion object {

        const val BYTE_SIZE = 3 * Float.SIZE_BYTES

        val zero = Vec3(0f, 0f, 0f)
        val one = Vec3(1f, 1f, 1f)
        val right = Vec3(1f, 0f, 0f)
        val left = Vec3(-1f, 0f, 0f)
        val up = Vec3(0f, 1f, 0f)
        val down = Vec3(0f, -1f, 0f)
        val forward = Vec3(0f, 0f, 1f)
        val backward = Vec3(0f, 0f, -1f)
    }
}
