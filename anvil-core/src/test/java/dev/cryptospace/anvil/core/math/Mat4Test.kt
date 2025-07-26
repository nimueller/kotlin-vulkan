package dev.cryptospace.anvil.core.math

import kotlin.test.Test
import kotlin.test.assertEquals

class Mat4Test {

    @Test
    fun `look at should return correct matrix`() {
        val eye = Vec3(0f, 0f, -1f)
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
        assertEquals(-1f, result[2, 3], 0.0001f)

        assertEquals(0f, result[3, 0], 0.0001f)
        assertEquals(0f, result[3, 1], 0.0001f)
        assertEquals(0f, result[3, 2], 0.0001f)
        assertEquals(1f, result[3, 3], 0.0001f)
    }
}
