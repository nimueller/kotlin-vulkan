package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class VertexLayoutTest {

    @Test
    fun `offsets and stride are derived from the attribute declarations`() {
        assertEquals(listOf(0, 12, 24), TexturedVertex3.attributes.map { it.offset })
        assertEquals(32, TexturedVertex3.byteSize)

        assertEquals(listOf(0, 8, 20), TexturedVertex2.attributes.map { it.offset })
        assertEquals(28, TexturedVertex2.byteSize)
    }

    @Test
    fun `locations follow declaration order`() {
        assertEquals(
            listOf(
                AttributeDescription(3, 0, AttributeFormat.SIGNED_FLOAT_3D, 0),
                AttributeDescription(3, 1, AttributeFormat.SIGNED_FLOAT_3D, 12),
                AttributeDescription(3, 2, AttributeFormat.SIGNED_FLOAT_2D, 24),
            ),
            TexturedVertex3.getAttributeDescriptions(binding = 3),
        )
    }

    @Test
    fun `writes vertices consecutively`() {
        val vertices = arrayOf(
            TexturedVertex2(Vec2(1f, 2f), Vec3(3f, 4f, 5f), Vec2(6f, 7f)),
            TexturedVertex2(Vec2(8f, 9f), Vec3(10f, 11f, 12f), Vec2(13f, 14f)),
        )
        val buffer = ByteBuffer.allocate(vertices.size * TexturedVertex2.byteSize).order(ByteOrder.nativeOrder())

        TexturedVertex2.write(vertices, buffer)

        val floats = (0 until buffer.capacity() / Float.SIZE_BYTES).map { buffer.getFloat(it * Float.SIZE_BYTES) }
        assertEquals((1..14).map { it.toFloat() }, floats)
    }
}
