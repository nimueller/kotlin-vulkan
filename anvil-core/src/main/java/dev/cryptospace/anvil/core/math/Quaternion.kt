package dev.cryptospace.anvil.core.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An immutable rotation quaternion `w + xi + yj + zk`.
 *
 * Accumulating rotations as quaternions and renormalizing avoids the drift that builds up when rotation
 * matrices are multiplied frame after frame.
 */
data class Quaternion(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {

    /**
     * Combines two rotations: the result applies [other] first, then this one.
     * With `rotation * delta`, `delta` is applied in the local space of `rotation`.
     */
    operator fun times(other: Quaternion): Quaternion = Quaternion(
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
        w = w * other.w - x * other.x - y * other.y - z * other.z,
    )

    fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

    fun normalized(): Quaternion {
        val length = length()
        return Quaternion(x / length, y / length, z / length, w / length)
    }

    companion object {

        val identity = Quaternion(0f, 0f, 0f, 1f)

        /**
         * Rotation by [angle] radians around [axis], which does not need to be normalized.
         */
        fun fromAxisAngle(axis: Vec3, angle: Float): Quaternion {
            val normalized = axis.normalized()
            val halfAngle = angle / 2f
            val sinHalf = sin(halfAngle)
            return Quaternion(normalized.x * sinHalf, normalized.y * sinHalf, normalized.z * sinHalf, cos(halfAngle))
        }
    }
}
