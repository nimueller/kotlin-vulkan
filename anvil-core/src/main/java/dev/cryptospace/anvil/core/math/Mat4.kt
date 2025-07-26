package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class Mat4(
    val row0: Vec4,
    val row1: Vec4,
    val row2: Vec4,
    val row3: Vec4,
) : NativeBuffer {

    val column0: Vec4 by lazy { Vec4(row0.x, row1.x, row2.x, row3.x) }
    val column1: Vec4 by lazy { Vec4(row0.y, row1.y, row2.y, row3.y) }
    val column2: Vec4 by lazy { Vec4(row0.z, row1.z, row2.z, row3.z) }
    val column3: Vec4 by lazy { Vec4(row0.w, row1.w, row2.w, row3.w) }

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        row0.toByteBuffer(byteBuffer)
        row1.toByteBuffer(byteBuffer)
        row2.toByteBuffer(byteBuffer)
        row3.toByteBuffer(byteBuffer)
    }

    fun translate(translation: Vec3): Mat4 = Mat4(
        row0 = row0,
        row1 = row1,
        row2 = row2,
        row3 = Vec4(row3.x + translation.x, row3.y + translation.y, row3.z + translation.z, row3.w),
    )

    fun rotate(angle: Float, axis: Vec3): Mat4 {
        val normalized = axis.normalized()
        val x = normalized.x
        val y = normalized.y
        val z = normalized.z

        val c = cos(angle)
        val s = sin(angle)
        val t = 1f - c

        return Mat4(
            row0 = Vec4(t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0f),
            row1 = Vec4(t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0f),
            row2 = Vec4(t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0f),
            row3 = Vec4(0f, 0f, 0f, 1f),
        )
    }

    fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
        val forward = (target - eye).normalized()
        val right = (forward cross up).normalized()
        val upward = right cross forward

        return Mat4(
            Vec4(right.x, upward.x, -forward.x, 0f),
            Vec4(right.y, upward.y, -forward.y, 0f),
            Vec4(right.z, upward.z, -forward.z, 0f),
            Vec4(-right.dot(eye), -upward.dot(eye), forward.dot(eye), 1f),
        )
    }

    operator fun times(other: Mat4): Mat4 = Mat4(
        row0 = row0 * other.row0,
        row1 = row1 * other.row1,
        row2 = row2 * other.row2,
        row3 = row3 * other.row3,
    )

    companion object {

        const val BYTE_SIZE = 4 * Vec4.BYTE_SIZE

        val identity = Mat4(
            row0 = Vec4(1f, 0f, 0f, 0f),
            row1 = Vec4(0f, 1f, 0f, 0f),
            row2 = Vec4(0f, 0f, 1f, 0f),
            row3 = Vec4(0f, 0f, 0f, 1f),
        )

        fun perspective(fov: Float, aspectRatio: Float, near: Float, far: Float): Mat4 {
            val tanHalfFov = tan(fov / 2f)
            return Mat4(
                Vec4(1f / (aspectRatio * tanHalfFov), 0f, 0f, 0f),
                Vec4(0f, 1f / tanHalfFov, 0f, 0f),
                Vec4(0f, 0f, (far + near) / (far - near), -1f),
                Vec4(0f, 0f, 2f * far * near / (far - near), 0f),
            )
        }
    }
}
