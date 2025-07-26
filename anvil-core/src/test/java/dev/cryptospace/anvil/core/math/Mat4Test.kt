package dev.cryptospace.anvil.core.math

import kotlin.test.Test
import kotlin.test.assertEquals

class Mat4Test {

    @Test
    fun `matrix times vector should return correct result`() {
        val mat = Mat4(
            row0 = Vec4(1f, 2f, 3f, 4f),
            row1 = Vec4(5f, 6f, 7f, 8f),
            row2 = Vec4(9f, 10f, 11f, 12f),
            row3 = Vec4(13f, 14f, 15f, 16f),
        )
        val vec = Vec4(17f, 18f, 19f, 20f)

        val result = mat * vec

        assertEquals(190f, result.x, 0.0001f)
        assertEquals(486f, result.y, 0.0001f)
        assertEquals(782f, result.z, 0.0001f)
        assertEquals(1078f, result.w, 0.0001f)
    }

    @Test
    fun `look at should return correct matrix`() {
        val eye = Vec3(0f, 0f, -0.5f)
        val center = Vec3(0f, 0f, 0f)
        val up = Vec3(0f, 1f, 0f)

        val result = Mat4.lookAt(eye, center, up)

        assertEquals(1f, result[0, 0], 0.0001f)
        assertEquals(0f, result[0, 1], 0.0001f)
        assertEquals(0f, result[0, 2], 0.0001f)
        assertEquals(0f, result[0, 3], 0.0001f)

        assertEquals(0f, result[1, 0], 0.0001f)
        assertEquals(1f, result[1, 1], 0.0001f)
        assertEquals(0f, result[1, 2], 0.0001f)
        assertEquals(0f, result[1, 3], 0.0001f)

        assertEquals(0f, result[2, 0], 0.0001f)
        assertEquals(0f, result[2, 1], 0.0001f)
        assertEquals(1f, result[2, 2], 0.0001f)
        assertEquals(0f, result[2, 3], 0.0001f)

        assertEquals(0f, result[3, 0], 0.0001f)
        assertEquals(0f, result[3, 1], 0.0001f)
        assertEquals(0.5f, result[3, 2], 0.0001f)
        assertEquals(1f, result[3, 3], 0.0001f)
    }
}
