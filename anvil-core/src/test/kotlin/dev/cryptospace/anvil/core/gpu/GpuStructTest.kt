package dev.cryptospace.anvil.core.gpu

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vec4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class GpuStructTest {

    private object Std430Mixed : GpuStruct(LayoutRule.STD430) {
        val scalar by float()
        val direction by vec3()
        val count by int()
        val color by vec4()
        val transform by mat4()
    }

    private object Scalar : GpuStruct(LayoutRule.SCALAR) {
        val a by float()
        val b by vec3()
        val c by vec4()
    }

    private object Continued : GpuStruct(LayoutRule.STD430, startOffset = 64) {
        val value by float()
        val tint by vec4()
    }

    @Test
    fun `std430 aligns vectors and matrices to 16 bytes`() {
        assertEquals(0, Std430Mixed.scalar.offset)
        assertEquals(16, Std430Mixed.direction.offset)
        // An int may fill the gap behind a vec3
        assertEquals(28, Std430Mixed.count.offset)
        assertEquals(32, Std430Mixed.color.offset)
        assertEquals(48, Std430Mixed.transform.offset)
        assertEquals(112, Std430Mixed.size)
    }

    @Test
    fun `field names come from the property names`() {
        assertEquals(listOf("scalar", "direction", "count", "color", "transform"), Std430Mixed.fields.map { it.name })
    }

    @Test
    fun `scalar layout packs tightly`() {
        assertEquals(listOf(0, 4, 16), Scalar.fields.map { it.offset })
        assertEquals(32, Scalar.size)
    }

    @Test
    fun `structs can continue at a start offset`() {
        assertEquals(64, Continued.value.offset)
        assertEquals(80, Continued.tint.offset)
        assertEquals(16, Continued.tint.bufferOffset)
        assertEquals(96, Continued.endOffset)
        assertEquals(32, Continued.size)
    }

    @Test
    fun `writes values at their offsets without moving the buffer position`() {
        val buffer = ByteBuffer.allocate(Std430Mixed.size).order(ByteOrder.nativeOrder())

        Std430Mixed.scalar.put(buffer, 1.5f)
        Std430Mixed.direction.put(buffer, Vec3(1f, 2f, 3f))
        Std430Mixed.count.put(buffer, 7)
        Std430Mixed.color.put(buffer, Vec4(4f, 5f, 6f, 7f))
        Std430Mixed.transform.put(buffer, Mat4.translation(Vec3(8f, 9f, 10f)))

        assertEquals(0, buffer.position())
        assertEquals(1.5f, buffer.getFloat(0))
        assertEquals(listOf(1f, 2f, 3f), (0..2).map { buffer.getFloat(16 + it * 4) })
        assertEquals(7, buffer.getInt(28))
        assertEquals(listOf(4f, 5f, 6f, 7f), (0..3).map { buffer.getFloat(32 + it * 4) })
        assertEquals(listOf(8f, 9f, 10f, 1f), (12..15).map { buffer.getFloat(48 + it * 4) })
    }

    @Test
    fun `writes relative to the start offset and an optional array element offset`() {
        val buffer = ByteBuffer.allocate(Continued.size * 2).order(ByteOrder.nativeOrder())

        Continued.value.put(buffer, 1f)
        Continued.value.put(buffer, Continued.size, 2f)

        assertEquals(1f, buffer.getFloat(0))
        assertEquals(2f, buffer.getFloat(Continued.size))
    }
}
