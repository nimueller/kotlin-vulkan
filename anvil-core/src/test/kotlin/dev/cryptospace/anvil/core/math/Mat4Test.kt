package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Mat4Test {

    private fun assertVecEquals(expected: Vec4, actual: Vec4, tolerance: Float = 1e-5f) {
        assertTrue(
            (0..3).all { index -> abs(expected[index] - actual[index]) <= tolerance },
            "Expected $expected but was $actual",
        )
    }

    @Test
    fun `translate keeps the homogeneous coordinate at 1`() {
        // Regression: translate used to add 1 to the w component, halving every translated position
        val translated = Mat4.identity.translate(Vec3(0f, 0f, -2f))

        assertVecEquals(Vec4(1f, 2f, 1f, 1f), translated * Vec4(1f, 2f, 3f, 1f))
        assertEquals(Vec4(0f, 0f, 0f, 1f), translated.row3)
    }

    @Test
    fun `translate composes in local space like rotate`() {
        // Rotating 90 degrees around Z, then moving along local X, ends up moving along world Y
        val transform = Mat4.identity
            .rotate((PI / 2).toFloat(), Vec3(0f, 0f, 1f))
            .translate(Vec3(1f, 0f, 0f))

        assertVecEquals(Vec4(0f, 1f, 0f, 1f), transform * Vec4(0f, 0f, 0f, 1f))
    }

    @Test
    fun `translation and scaling factories`() {
        val transform = Mat4.translation(Vec3(1f, 2f, 3f)) * Mat4.scaling(Vec3(2f, 3f, 4f))

        assertVecEquals(Vec4(3f, 5f, 7f, 1f), transform * Vec4(1f, 1f, 1f, 1f))
    }

    @Test
    fun `identity is the neutral element of multiplication`() {
        val transform = Mat4.translation(Vec3(1f, 2f, 3f)).rotate(0.3f, Vec3(1f, 1f, 0f))

        assertEquals(transform, Mat4.identity * transform)
        assertEquals(transform, transform * Mat4.identity)
    }

    @Test
    fun `writes column major data at an absolute offset`() {
        val buffer = ByteBuffer.allocate(8 + Mat4.BYTE_SIZE)

        Mat4.translation(Vec3(1f, 2f, 3f)).putColumnMajor(buffer, 8)

        assertEquals(0, buffer.position())
        // The translation lives in the last column, which comes last in column-major order
        val floats = (0 until 16).map { index -> buffer.getFloat(8 + index * Float.SIZE_BYTES) }
        assertEquals(listOf(1f, 0f, 0f, 0f), floats.subList(0, 4))
        assertEquals(listOf(1f, 2f, 3f, 1f), floats.subList(12, 16))
    }

    @Test
    fun `multiplication matches the row and column definition`() {
        val a = Mat4.translation(Vec3(1f, 2f, 3f)).rotate(0.7f, Vec3(1f, 2f, 3f)).scale(Vec3(2f, 3f, 4f))
        val b = Mat4.identity.rotate(-1.3f, Vec3(0f, 1f, 1f)).translate(Vec3(-4f, 5f, 0.5f))

        val product = a * b

        for (row in 0..3) {
            for (column in 0..3) {
                assertEquals(a.getRowVector(row) dot b.getColumnVector(column), product[row, column], 1e-5f)
            }
        }
    }

    @Test
    fun `translationRotationScale equals T times R times S`() {
        val rotation = Quaternion.fromAxisAngle(Vec3(1f, -2f, 0.5f), 1.1f)
        val expected = Mat4.translation(Vec3(1f, 2f, 3f)) * Mat4.rotation(rotation) * Mat4.scaling(Vec3(2f, 3f, 4f))

        val actual = Mat4.translationRotationScale(Vec3(1f, 2f, 3f), rotation, Vec3(2f, 3f, 4f))

        assertMatrixEquals(expected, actual)
    }

    @Test
    fun `rotate matches the axis angle rotation`() {
        // 90 degrees around Z maps X onto Y
        val rotated = Mat4.identity.rotate((PI / 2).toFloat(), Vec3(0f, 0f, 2f))

        assertVecEquals(Vec4(0f, 1f, 0f, 0f), rotated * Vec4(1f, 0f, 0f, 0f))
    }

    private fun Mat4.getRowVector(row: Int) = Vec4(this[row, 0], this[row, 1], this[row, 2], this[row, 3])

    private fun Mat4.getColumnVector(column: Int) =
        Vec4(this[0, column], this[1, column], this[2, column], this[3, column])

    private fun assertMatrixEquals(expected: Mat4, actual: Mat4) {
        for (row in 0..3) {
            assertVecEquals(
                Vec4(expected[row, 0], expected[row, 1], expected[row, 2], expected[row, 3]),
                Vec4(actual[row, 0], actual[row, 1], actual[row, 2], actual[row, 3]),
            )
        }
    }
}
