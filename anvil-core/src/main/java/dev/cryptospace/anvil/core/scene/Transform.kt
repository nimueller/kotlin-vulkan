package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.MatrixSource
import dev.cryptospace.anvil.core.math.Quaternion
import dev.cryptospace.anvil.core.math.Vec3
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Position, rotation and scale of an object.
 *
 * The model matrix `T * R * S` is rebuilt in place only when a component changed, so static objects cost nothing
 * per frame, and accumulated rotations stay normalized instead of drifting. Rotation and matrix are kept as
 * primitive fields, so [rotate] and writing the matrix to the GPU do not allocate.
 */
class Transform(
    position: Vec3 = Vec3.zero,
    rotation: Quaternion = Quaternion.identity,
    scale: Vec3 = Vec3.one,
) : MatrixSource {

    var position: Vec3 = position
        set(value) {
            field = value
            dirty = true
        }

    var scale: Vec3 = scale
        set(value) {
            field = value
            dirty = true
        }

    private var rotationX = 0f
    private var rotationY = 0f
    private var rotationZ = 0f
    private var rotationW = 1f

    /** Must be a unit quaternion. [rotate] keeps it normalized. */
    var rotation: Quaternion
        get() = Quaternion(rotationX, rotationY, rotationZ, rotationW)
        set(value) {
            rotationX = value.x
            rotationY = value.y
            rotationZ = value.z
            rotationW = value.w
            dirty = true
        }

    /** Column-major `T * R * S`, valid unless [dirty]. */
    private val columnMajor = FloatArray(MATRIX_ELEMENTS)
    private var dirty = true

    init {
        this.rotation = rotation
    }

    /**
     * The model matrix `T * R * S`. Allocates a new [Mat4] on every access; renderers use [putColumnMajor] instead.
     */
    val matrix: Mat4
        get() {
            updateMatrix()
            val m = columnMajor
            return Mat4(
                m[0], m[4], m[8], m[12],
                m[1], m[5], m[9], m[13],
                m[2], m[6], m[10], m[14],
                m[3], m[7], m[11], m[15],
            )
        }

    override fun putColumnMajor(buffer: ByteBuffer, offset: Int) {
        updateMatrix()
        for (index in 0 until MATRIX_ELEMENTS) {
            buffer.putFloat(offset + index * Float.SIZE_BYTES, columnMajor[index])
        }
    }

    /**
     * Rotates by [angle] radians around [axis], given in the object's local space.
     */
    fun rotate(angle: Float, axis: Vec3) {
        val axisLength = sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z)
        val sinHalf = sin(angle / 2f) / axisLength
        val deltaX = axis.x * sinHalf
        val deltaY = axis.y * sinHalf
        val deltaZ = axis.z * sinHalf
        val deltaW = cos(angle / 2f)

        // rotation * delta applies delta in local space, see Quaternion.times
        val x = rotationW * deltaX + rotationX * deltaW + rotationY * deltaZ - rotationZ * deltaY
        val y = rotationW * deltaY - rotationX * deltaZ + rotationY * deltaW + rotationZ * deltaX
        val z = rotationW * deltaZ + rotationX * deltaY - rotationY * deltaX + rotationZ * deltaW
        val w = rotationW * deltaW - rotationX * deltaX - rotationY * deltaY - rotationZ * deltaZ

        val length = sqrt(x * x + y * y + z * z + w * w)
        rotationX = x / length
        rotationY = y / length
        rotationZ = z / length
        rotationW = w / length
        dirty = true
    }

    /**
     * Moves by [offset], given in world space.
     */
    fun translate(offset: Vec3) {
        position += offset
    }

    private fun updateMatrix() {
        if (!dirty) return

        val x = rotationX
        val y = rotationY
        val z = rotationZ
        val w = rotationW
        val scaleX = scale.x
        val scaleY = scale.y
        val scaleZ = scale.z
        val m = columnMajor

        // Column 0: rotated and scaled X axis
        m[0] = (1f - 2f * (y * y + z * z)) * scaleX
        m[1] = 2f * (x * y + w * z) * scaleX
        m[2] = 2f * (x * z - w * y) * scaleX
        m[3] = 0f
        // Column 1: rotated and scaled Y axis
        m[4] = 2f * (x * y - w * z) * scaleY
        m[5] = (1f - 2f * (x * x + z * z)) * scaleY
        m[6] = 2f * (y * z + w * x) * scaleY
        m[7] = 0f
        // Column 2: rotated and scaled Z axis
        m[8] = 2f * (x * z + w * y) * scaleZ
        m[9] = 2f * (y * z - w * x) * scaleZ
        m[10] = (1f - 2f * (x * x + y * y)) * scaleZ
        m[11] = 0f
        // Column 3: translation
        m[12] = position.x
        m[13] = position.y
        m[14] = position.z
        m[15] = 1f

        dirty = false
    }

    private companion object {
        const val MATRIX_ELEMENTS = 16
    }
}
