package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.scene.MaterialProperties
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that the CPU writes push constants exactly where the compiled shaders read them.
 */
class PushConstantsLayoutTest {

    private val properties = MaterialProperties(
        albedo = Vec4(0.1f, 0.2f, 0.3f, 0.4f),
        roughness = 0.5f,
        metallic = 0.6f,
        emissive = 0.7f,
        ao = 0.8f,
    )
    private val textureIndex = 42

    private val fragmentBytes: ByteBuffer = ByteBuffer.allocate(PushConstants.FRAGMENT_SIZE)
        .order(ByteOrder.nativeOrder())
        .also { buffer -> PushConstants.writeFragment(buffer, properties, textureIndex) }

    private fun floatAt(shaderOffset: Int) = fragmentBytes.getFloat(shaderOffset - PushConstants.FRAGMENT_OFFSET)

    @ParameterizedTest
    @ValueSource(strings = ["/shaders/shader.frag.spv", "/shaders/shader2D.frag.spv"])
    fun `fragment shader reads the values the CPU writes`(shader: String) {
        val offsets = SpirvReflection.fromResource(shader).memberOffsets("PushConstants")

        assertEquals(setOf("albedo", "roughness", "metallic", "emissive", "ao", "textureIndex"), offsets.keys)
        assertEquals(
            listOf(0.1f, 0.2f, 0.3f, 0.4f),
            (0 until 4).map { component -> floatAt(offsets.getValue("albedo") + component * Float.SIZE_BYTES) },
        )
        assertEquals(0.5f, floatAt(offsets.getValue("roughness")))
        assertEquals(0.6f, floatAt(offsets.getValue("metallic")))
        assertEquals(0.7f, floatAt(offsets.getValue("emissive")))
        assertEquals(0.8f, floatAt(offsets.getValue("ao")))
        assertEquals(
            textureIndex,
            fragmentBytes.getInt(offsets.getValue("textureIndex") - PushConstants.FRAGMENT_OFFSET),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["/shaders/shader.frag.spv", "/shaders/shader2D.frag.spv"])
    fun `fragment block fits the declared push constant range`(shader: String) {
        val offsets = SpirvReflection.fromResource(shader).memberOffsets("PushConstants")

        assertEquals(PushConstants.FRAGMENT_OFFSET, offsets.values.min())
        val blockEnd = offsets.getValue("textureIndex") + Int.SIZE_BYTES
        assertEquals(PushConstants.FRAGMENT_OFFSET + PushConstants.FRAGMENT_SIZE, blockEnd)
    }

    @ParameterizedTest
    @ValueSource(strings = ["/shaders/shader.vert.spv", "/shaders/shader2D.vert.spv"])
    fun `vertex shader reads the model matrix at the vertex offset`(shader: String) {
        val offsets = SpirvReflection.fromResource(shader).memberOffsets("PushConstants")

        assertEquals(mapOf("model" to PushConstants.VERTEX_OFFSET), offsets)
    }

    @Test
    fun `ranges stay within the guaranteed push constant limit`() {
        // Vulkan guarantees at least 128 bytes of push constants on every device
        assertTrue(PushConstants.FRAGMENT_OFFSET + PushConstants.FRAGMENT_SIZE <= 128)
        assertEquals(PushConstants.VERTEX_OFFSET + PushConstants.VERTEX_SIZE, PushConstants.FRAGMENT_OFFSET)
    }
}
