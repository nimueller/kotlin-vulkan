package dev.cryptospace.anvil.core.math

import org.lwjgl.system.MemoryUtil
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
    fun `writes column major data`() {
        val buffer = Mat4.translation(Vec3(1f, 2f, 3f)).toByteBuffer()

        try {
            // The translation lives in the last column, which comes last in column-major order
            val floats = (0 until 16).map { index -> buffer.getFloat(index * Float.SIZE_BYTES) }
            assertEquals(listOf(1f, 2f, 3f, 1f), floats.subList(12, 16))
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }
}
