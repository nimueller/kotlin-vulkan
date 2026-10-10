package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.math.tan

/**
 * An immutable 4x4 matrix. Element `mRC` is in row `R` and column `C`, so vectors are columns and
 * transformations compose right to left: `(A * B) * v` applies `B` first.
 *
 * Stored as plain fields, so every operation allocates exactly one object. Graphics APIs expect column-major data,
 * which [putColumnMajor] writes without allocating.
 */
data class Mat4(
    val m00: Float,
    val m01: Float,
    val m02: Float,
    val m03: Float,
    val m10: Float,
    val m11: Float,
    val m12: Float,
    val m13: Float,
    val m20: Float,
    val m21: Float,
    val m22: Float,
    val m23: Float,
    val m30: Float,
    val m31: Float,
    val m32: Float,
    val m33: Float,
) : MatrixSource {

    constructor(row0: Vec4, row1: Vec4, row2: Vec4, row3: Vec4) : this(
        row0.x, row0.y, row0.z, row0.w,
        row1.x, row1.y, row1.z, row1.w,
        row2.x, row2.y, row2.z, row2.w,
        row3.x, row3.y, row3.z, row3.w,
    )

    val row0: Vec4 get() = Vec4(m00, m01, m02, m03)
    val row1: Vec4 get() = Vec4(m10, m11, m12, m13)
    val row2: Vec4 get() = Vec4(m20, m21, m22, m23)
    val row3: Vec4 get() = Vec4(m30, m31, m32, m33)

    val column0: Vec4 get() = Vec4(m00, m10, m20, m30)
    val column1: Vec4 get() = Vec4(m01, m11, m21, m31)
    val column2: Vec4 get() = Vec4(m02, m12, m22, m32)
    val column3: Vec4 get() = Vec4(m03, m13, m23, m33)

    /**
     * Writes the matrix in column-major order at the absolute [offset], leaving the buffer's position unchanged.
     */
    override fun putColumnMajor(buffer: ByteBuffer, offset: Int) {
        buffer.putFloat(offset, m00)
        buffer.putFloat(offset + 4, m10)
        buffer.putFloat(offset + 8, m20)
        buffer.putFloat(offset + 12, m30)
        buffer.putFloat(offset + 16, m01)
        buffer.putFloat(offset + 20, m11)
        buffer.putFloat(offset + 24, m21)
        buffer.putFloat(offset + 28, m31)
        buffer.putFloat(offset + 32, m02)
        buffer.putFloat(offset + 36, m12)
        buffer.putFloat(offset + 40, m22)
        buffer.putFloat(offset + 44, m32)
        buffer.putFloat(offset + 48, m03)
        buffer.putFloat(offset + 52, m13)
        buffer.putFloat(offset + 56, m23)
        buffer.putFloat(offset + 60, m33)
    }

    /**
     * Applies a translation in this matrix's local space (`this * T`), consistent with [rotate] and [scale].
     */
    fun translate(translation: Vec3): Mat4 = copy(
        m03 = m00 * translation.x + m01 * translation.y + m02 * translation.z + m03,
        m13 = m10 * translation.x + m11 * translation.y + m12 * translation.z + m13,
        m23 = m20 * translation.x + m21 * translation.y + m22 * translation.z + m23,
        m33 = m30 * translation.x + m31 * translation.y + m32 * translation.z + m33,
    )

    /**
     * Applies a scale in this matrix's local space (`this * S`).
     */
    fun scale(scale: Vec3): Mat4 = copy(
        m00 = m00 * scale.x,
        m10 = m10 * scale.x,
        m20 = m20 * scale.x,
        m30 = m30 * scale.x,
        m01 = m01 * scale.y,
        m11 = m11 * scale.y,
        m21 = m21 * scale.y,
        m31 = m31 * scale.y,
        m02 = m02 * scale.z,
        m12 = m12 * scale.z,
        m22 = m22 * scale.z,
        m32 = m32 * scale.z,
    )

    /**
     * Applies a rotation around [axis] by [angle] radians in this matrix's local space (`this * R`).
     */
    fun rotate(angle: Float, axis: Vec3): Mat4 = this * rotation(Quaternion.fromAxisAngle(axis, angle))

    operator fun times(other: Mat4): Mat4 = Mat4(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30,
        m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31,
        m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32,
        m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33,
        m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30,
        m10 * other.m01 + m11 * other.m11 + m12 * other.m21 + m13 * other.m31,
        m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32,
        m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33,
        m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30,
        m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31,
        m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32,
        m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33,
        m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30,
        m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31,
        m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32,
        m30 * other.m03 + m31 * other.m13 + m32 * other.m23 + m33 * other.m33,
    )

    operator fun times(other: Vec4): Vec4 = Vec4(
        x = m00 * other.x + m01 * other.y + m02 * other.z + m03 * other.w,
        y = m10 * other.x + m11 * other.y + m12 * other.z + m13 * other.w,
        z = m20 * other.x + m21 * other.y + m22 * other.z + m23 * other.w,
        w = m30 * other.x + m31 * other.y + m32 * other.z + m33 * other.w,
    )

    operator fun get(row: Int, column: Int): Float = when (row * 4 + column) {
        0 -> m00
        1 -> m01
        2 -> m02
        3 -> m03
        4 -> m10
        5 -> m11
        6 -> m12
        7 -> m13
        8 -> m20
        9 -> m21
        10 -> m22
        11 -> m23
        12 -> m30
        13 -> m31
        14 -> m32
        15 -> m33
        else -> throw IndexOutOfBoundsException("row and column must be in range [0, 3], were $row, $column")
    }

    companion object {

        const val BYTE_SIZE = 16 * Float.SIZE_BYTES

        val identity = Mat4(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )

        fun translation(translation: Vec3): Mat4 = Mat4(
            1f, 0f, 0f, translation.x,
            0f, 1f, 0f, translation.y,
            0f, 0f, 1f, translation.z,
            0f, 0f, 0f, 1f,
        )

        fun scaling(scale: Vec3): Mat4 = Mat4(
            scale.x, 0f, 0f, 0f,
            0f, scale.y, 0f, 0f,
            0f, 0f, scale.z, 0f,
            0f, 0f, 0f, 1f,
        )

        fun rotation(rotation: Quaternion): Mat4 = translationRotationScale(Vec3.zero, rotation, Vec3.one)

        /**
         * Builds `T * R * S` directly, which is the usual model matrix: scale first, then rotate, then translate.
         * Costs a single allocation, unlike multiplying the three matrices.
         *
         * @param rotation Must be normalized
         */
        fun translationRotationScale(translation: Vec3, rotation: Quaternion, scale: Vec3): Mat4 {
            val x = rotation.x
            val y = rotation.y
            val z = rotation.z
            val w = rotation.w
            val xx = x * x
            val yy = y * y
            val zz = z * z
            val xy = x * y
            val xz = x * z
            val yz = y * z
            val wx = w * x
            val wy = w * y
            val wz = w * z

            return Mat4(
                (1f - 2f * (yy + zz)) * scale.x,
                2f * (xy - wz) * scale.y,
                2f * (xz + wy) * scale.z,
                translation.x,
                2f * (xy + wz) * scale.x,
                (1f - 2f * (xx + zz)) * scale.y,
                2f * (yz - wx) * scale.z,
                translation.y,
                2f * (xz - wy) * scale.x,
                2f * (yz + wx) * scale.y,
                (1f - 2f * (xx + yy)) * scale.z,
                translation.z,
                0f,
                0f,
                0f,
                1f,
            )
        }

        /**
         * Creates a right-handed view matrix using OpenGL coordinate system conventions.
         *
         * In OpenGL's right-handed coordinate system:
         * - Positive X points right
         * - Positive Y points up
         * - Negative Z points forward (into the screen)
         *
         * @param eye Position of the camera in world space
         * @param target Position the camera is looking at in world space
         * @param up Approximate up direction in world space (will be orthonormalized)
         * @return View transformation matrix that transforms world space to view space
         */
        fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
            val direction = (target - eye).normalized()
            val left = (direction cross up).normalized()
            val upNormalized = left cross direction

            return Mat4(
                row0 = Vec4(x = left.x, y = left.y, z = left.z, w = -(left dot eye)),
                row1 = Vec4(x = upNormalized.x, y = upNormalized.y, z = upNormalized.z, w = -(upNormalized dot eye)),
                row2 = Vec4(x = -direction.x, y = -direction.y, z = -direction.z, w = (direction dot eye)),
                row3 = Vec4(x = 0f, y = 0f, z = 0f, w = 1f),
            )
        }

        /**
         * Creates a perspective projection matrix for OpenGL using a right-handed coordinate system,
         * where the camera looks down the -Z axis.
         *
         * The resulting matrix maps depth to OpenGL's clip space (Z from -1 to 1).
         *
         * @param fov Vertical field of view in degrees
         * @param aspectRatio Width divided by height of viewport
         * @param near Distance to the near clipping plane
         * @param far Distance to the far clipping plane
         * @return OpenGL-compatible perspective projection matrix (right-handed view, depth -1..1)
         */
        fun perspectiveOpenGL(fov: Float, aspectRatio: Float, near: Float, far: Float): Mat4 {
            val focalLength = 1f / tan(Math.toRadians(fov.toDouble()).toFloat() / 2f)
            val zScale = (far + near) / (far - near)
            val zTranslation = 2f * far * near / (far - near)

            return Mat4(
                row0 = Vec4(x = focalLength / aspectRatio, y = 0f, z = 0f, w = 0f),
                row1 = Vec4(x = 0f, y = focalLength, z = 0f, w = 0f),
                row2 = Vec4(x = 0f, y = 0f, z = zScale, w = -1f),
                row3 = Vec4(x = 0f, y = 0f, z = zTranslation, w = 0f),
            )
        }

        /**
         * Creates a perspective projection matrix for Vulkan using a right-handed coordinate system,
         * where the camera looks down the -Z axis (OpenGL-style).
         *
         * The resulting matrix maps depth to Vulkan's clip space (Z from 0 to 1).
         *
         * **Note:** The Y axis is flipped (negative focal length) to account for Vulkan's
         * coordinate system, where the origin is in the top-left and Y points down in NDC.
         *
         * @param fov Vertical field of view in degrees
         * @param aspectRatio Width divided by height of viewport
         * @param near Distance to the near clipping plane
         * @param far Distance to the far clipping plane
         * @return Vulkan-compatible perspective projection matrix (right-handed view, depth 0..1)
         */
        fun perspectiveVulkan(fov: Float, aspectRatio: Float, near: Float, far: Float): Mat4 {
            val focalLength = 1f / tan(Math.toRadians(fov.toDouble()).toFloat() / 2f)
            val zScale = far / (near - far)
            val zTranslation = (near * far) / (near - far)

            return Mat4(
                row0 = Vec4(focalLength / aspectRatio, 0f, 0f, 0f),
                row1 = Vec4(0f, -focalLength, 0f, 0f),
                row2 = Vec4(0f, 0f, zScale, zTranslation),
                row3 = Vec4(0f, 0f, -1f, 0f),
            )
        }
    }
}
