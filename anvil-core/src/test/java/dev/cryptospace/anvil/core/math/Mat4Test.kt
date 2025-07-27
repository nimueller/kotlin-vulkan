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
}
