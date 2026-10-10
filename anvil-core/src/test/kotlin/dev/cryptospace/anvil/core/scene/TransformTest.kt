package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Quaternion
import dev.cryptospace.anvil.core.math.Vec3
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransformTest {

    private fun assertMatrixEquals(expected: Mat4, actual: Mat4, tolerance: Float = 1e-5f) {
        for (row in 0..3) {
            for (column in 0..3) {
                assertTrue(
                    abs(expected[row, column] - actual[row, column]) <= tolerance,
                    "Element ($row, $column): expected ${expected[row, column]} but was ${actual[row, column]}",
                )
            }
        }
    }

    @Test
    fun `defaults to the identity`() {
        assertEquals(Mat4.identity, Transform().matrix)
    }

    @Test
    fun `matrix is translation times rotation times scale`() {
        val rotation = Quaternion.fromAxisAngle(Vec3(1f, 2f, 3f), 0.8f)
        val transform = Transform(position = Vec3(1f, 2f, 3f), rotation = rotation, scale = Vec3(2f, 3f, 4f))

        assertMatrixEquals(
            Mat4.translationRotationScale(Vec3(1f, 2f, 3f), rotation, Vec3(2f, 3f, 4f)),
            transform.matrix,
        )
    }

    @Test
    fun `matrix follows component changes`() {
        val transform = Transform()
        transform.matrix

        transform.position = Vec3(5f, 0f, 0f)
        assertEquals(5f, transform.matrix[0, 3])

        transform.scale = Vec3(2f, 2f, 2f)
        assertEquals(2f, transform.matrix[0, 0])

        transform.translate(Vec3(1f, 1f, 0f))
        assertEquals(Vec3(6f, 1f, 0f), transform.position)
        assertEquals(1f, transform.matrix[1, 3])
    }

    @Test
    fun `rotate applies the rotation in local space like quaternion multiplication`() {
        val start = Quaternion.fromAxisAngle(Vec3(0f, 1f, 0f), 0.4f)
        val transform = Transform(rotation = start)

        transform.rotate(1.2f, Vec3(0f, 0f, 3f))

        val expected = start * Quaternion.fromAxisAngle(Vec3(0f, 0f, 1f), 1.2f)
        assertMatrixEquals(Mat4.rotation(expected), transform.matrix)
    }

    @Test
    fun `many small rotations stay normalized and accurate`() {
        val transform = Transform()
        val steps = 100_000

        repeat(steps) { transform.rotate((2 * PI / steps).toFloat(), Vec3(0f, 0f, 1f)) }

        assertEquals(1f, transform.rotation.length(), 1e-5f)
        // A full turn returns to the start
        assertMatrixEquals(Mat4.identity, transform.matrix, tolerance = 1e-3f)
    }

    @Test
    fun `writes the same matrix to buffers as the matrix property`() {
        val transform = Transform(position = Vec3(1f, 2f, 3f), scale = Vec3(4f, 5f, 6f))
        transform.rotate(0.3f, Vec3(1f, 0f, 0f))
        val written = ByteBuffer.allocate(Mat4.BYTE_SIZE)
        val expected = ByteBuffer.allocate(Mat4.BYTE_SIZE)

        transform.putColumnMajor(written, 0)
        transform.matrix.putColumnMajor(expected, 0)

        assertEquals(expected, written)
    }
}
