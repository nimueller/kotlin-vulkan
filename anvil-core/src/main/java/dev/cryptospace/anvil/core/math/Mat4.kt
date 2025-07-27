package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * A 4x4 matrix that stores data in row-major order for more natural and intuitive matrix operations.
 *
 * The matrix is stored internally using four Vec4 objects representing rows, making operations like
 * matrix multiplication and transformations more straightforward. However, when writing to byte
 * buffers (e.g., for graphics APIs), the data can be automatically converted to column-major order
 * as required by OpenGL and Vulkan.
 *
 * This dual representation allows for natural mathematical operations while maintaining compatibility
 * with graphics APIs that expect column-major data.
 */
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
        toByteBuffer(byteBuffer, columnMajor = true)
    }

    /**
     * Writes the matrix data to a ByteBuffer in either column-major or row-major order.
     *
     * @param byteBuffer The buffer to write the matrix data into
     * @param columnMajor If true, writes data in column-major order (default for OpenGL/Vulkan).
     *                    If false, writes in row-major order.
     */
    fun toByteBuffer(byteBuffer: ByteBuffer, columnMajor: Boolean) {
        if (columnMajor) {
            column0.toByteBuffer(byteBuffer)
            column1.toByteBuffer(byteBuffer)
            column2.toByteBuffer(byteBuffer)
            column3.toByteBuffer(byteBuffer)
        } else {
            row0.toByteBuffer(byteBuffer)
            row1.toByteBuffer(byteBuffer)
            row2.toByteBuffer(byteBuffer)
            row3.toByteBuffer(byteBuffer)
        }
    }

    fun translate(translation: Vec3): Mat4 = translate(Vec4(translation, 1f))

    fun translate(translation: Vec4): Mat4 = Mat4(
        row0 = Vec4(row0.x, row0.y, row0.z, row0.w + translation.x),
        row1 = Vec4(row1.x, row1.y, row1.z, row1.w + translation.y),
        row2 = Vec4(row2.x, row2.y, row2.z, row2.w + translation.z),
        row3 = Vec4(row3.x, row3.y, row3.z, row3.w + translation.w),
    )

    fun scale(scale: Vec3): Mat4 = scale(Vec4(scale, 1f))

    fun scale(scale: Vec4): Mat4 = Mat4(
        row0 = Vec4(row0.x * scale.x, row0.y * scale.y, row0.z * scale.z, row0.w),
        row1 = Vec4(row1.x * scale.x, row1.y * scale.y, row1.z * scale.z, row1.w),
        row2 = Vec4(row2.x * scale.x, row2.y * scale.y, row2.z * scale.z, row2.w),
        row3 = Vec4(row3.x * scale.w, row3.y * scale.w, row3.z * scale.w, row3.w),
    )

    /**
     * Creates a rotation matrix that rotates around the specified axis by the given angle.
     *
     * This function uses Rodrigues' rotation formula to create a rotation matrix that can be
     * used to rotate vectors or compose with other transformations. The axis vector will be
     * automatically normalized.
     *
     * @param angle The rotation angle in radians
     * @param axis The vector representing the axis of rotation
     * @return A new Mat4 representing the rotation transformation
     */
    fun rotate(angle: Float, axis: Vec3): Mat4 {
        val normalized = axis.normalized()
        val x = normalized.x
        val y = normalized.y
        val z = normalized.z

        val c = cos(angle)
        val s = sin(angle)
        val t = 1f - c

        val rotation = Mat4(
            row0 = Vec4(t * x * x + c, t * x * y - s * z, t * x * z + s * y, 0f),
            row1 = Vec4(t * x * y + s * z, t * y * y + c, t * y * z - s * x, 0f),
            row2 = Vec4(t * x * z - s * y, t * y * z + s * x, t * z * z + c, 0f),
            row3 = Vec4(0f, 0f, 0f, 1f),
        )

        return this * rotation
    }

    operator fun times(other: Mat4): Mat4 = Mat4(
        row0 = Vec4(row0.dot(other.column0), row0.dot(other.column1), row0.dot(other.column2), row0.dot(other.column3)),
        row1 = Vec4(row1.dot(other.column0), row1.dot(other.column1), row1.dot(other.column2), row1.dot(other.column3)),
        row2 = Vec4(row2.dot(other.column0), row2.dot(other.column1), row2.dot(other.column2), row2.dot(other.column3)),
        row3 = Vec4(row3.dot(other.column0), row3.dot(other.column1), row3.dot(other.column2), row3.dot(other.column3)),
    )

    operator fun times(other: Vec4): Vec4 = Vec4(
        x = row0.dot(other),
        y = row1.dot(other),
        z = row2.dot(other),
        w = row3.dot(other),
    )

    operator fun get(row: Int, column: Int): Float = when (row) {
        0 -> row0[column]
        1 -> row1[column]
        2 -> row2[column]
        3 -> row3[column]
        else -> throw IndexOutOfBoundsException("row must be in range [0, 3]")
    }

    companion object {

        const val BYTE_SIZE = 4 * Vec4.BYTE_SIZE

        val identity = Mat4(
            row0 = Vec4(1f, 0f, 0f, 0f),
            row1 = Vec4(0f, 1f, 0f, 0f),
            row2 = Vec4(0f, 0f, 1f, 0f),
            row3 = Vec4(0f, 0f, 0f, 1f),
        )

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
