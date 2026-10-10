package dev.cryptospace.anvil.core.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuaternionTest {

    private fun rotate(quaternion: Quaternion, vector: Vec3): Vec3 {
        val rotated = Mat4.rotation(quaternion) * Vec4(vector, 0f)
        return Vec3(rotated.x, rotated.y, rotated.z)
    }

    private fun assertVecEquals(expected: Vec3, actual: Vec3) {
        assertTrue(
            abs(expected.x - actual.x) < 1e-5f &&
                abs(expected.y - actual.y) < 1e-5f &&
                abs(expected.z - actual.z) < 1e-5f,
            "Expected $expected but was $actual",
        )
    }

    @Test
    fun `axis angle rotation follows the right hand rule`() {
        val quarterTurnAroundZ = Quaternion.fromAxisAngle(Vec3(0f, 0f, 5f), (PI / 2).toFloat())

        assertVecEquals(Vec3(0f, 1f, 0f), rotate(quarterTurnAroundZ, Vec3(1f, 0f, 0f)))
        assertEquals(1f, quarterTurnAroundZ.length(), 1e-6f)
    }

    @Test
    fun `product applies the right operand first`() {
        val aroundZ = Quaternion.fromAxisAngle(Vec3(0f, 0f, 1f), (PI / 2).toFloat())
        val aroundX = Quaternion.fromAxisAngle(Vec3(1f, 0f, 0f), (PI / 2).toFloat())

        // X around X stays X, then Z turns it into Y
        assertVecEquals(Vec3(0f, 1f, 0f), rotate(aroundZ * aroundX, Vec3(1f, 0f, 0f)))
        // Y around X becomes Z, which Z leaves alone
        assertVecEquals(Vec3(0f, 0f, 1f), rotate(aroundZ * aroundX, Vec3(0f, 1f, 0f)))
    }

    @Test
    fun `identity does not rotate`() {
        assertVecEquals(Vec3(1f, 2f, 3f), rotate(Quaternion.identity, Vec3(1f, 2f, 3f)))
        assertEquals(Mat4.identity, Mat4.rotation(Quaternion.identity))
    }
}
