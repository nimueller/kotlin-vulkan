package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.math.Vec4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialPropertiesTest {

    @Test
    fun `serializes albedo followed by the scalar properties`() {
        val properties = MaterialProperties(
            albedo = Vec4(0.1f, 0.2f, 0.3f, 0.4f),
            roughness = 0.5f,
            metallic = 0.6f,
            emissive = 0.7f,
            ao = 0.8f,
        )
        val buffer = ByteBuffer.allocate(MaterialProperties.BYTE_SIZE).order(ByteOrder.nativeOrder())

        properties.toByteBuffer(buffer)

        assertEquals(MaterialProperties.BYTE_SIZE, buffer.position())
        val floats = (0 until 8).map { index -> buffer.getFloat(index * Float.SIZE_BYTES) }
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f), floats)
    }
}
