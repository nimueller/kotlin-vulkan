package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.AttributeDescription
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.scene.BlendMode
import dev.cryptospace.anvil.core.scene.CullMode
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialProperties
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PipelineKeyTest {

    private val defaults3D = PipelineKey.ShaderPair(ShaderId(1), ShaderId(2))
    private val defaults2D = PipelineKey.ShaderPair(ShaderId(3), ShaderId(4))

    private fun defaults(layout: VertexLayout<*>) = when (layout) {
        TexturedVertex3 -> defaults3D
        TexturedVertex2 -> defaults2D
        else -> null
    }

    private fun keyOf(material: Material?, layout: VertexLayout<*> = TexturedVertex3) =
        PipelineKey.of(layout, material, ::defaults)

    @Test
    fun `materials differing only in parameters share a pipeline`() {
        val plain = Material()
        val textured = Material(
            texture = TextureId(5),
            properties = MaterialProperties(albedo = Vec4(1f, 0f, 0f, 1f), roughness = 0.9f),
        )

        assertEquals(keyOf(plain), keyOf(textured))
        assertEquals(keyOf(null), keyOf(plain))
    }

    @Test
    fun `render state changes require a separate pipeline`() {
        val base = keyOf(Material())

        assertNotEquals(base, keyOf(Material(blendMode = BlendMode.ALPHA_BLEND)))
        assertNotEquals(base, keyOf(Material(cullMode = CullMode.NONE)))
        assertNotEquals(base, keyOf(Material(depthTest = false)))
        assertNotEquals(base, keyOf(Material(depthWrite = false)))
    }

    @Test
    fun `vertex layout selects the default shaders`() {
        assertEquals(defaults3D.vertex, keyOf(null, TexturedVertex3).vertexShader)
        assertEquals(defaults2D.fragment, keyOf(null, TexturedVertex2).fragmentShader)
        assertNotEquals(keyOf(null, TexturedVertex3), keyOf(null, TexturedVertex2))
    }

    @Test
    fun `material shaders override the defaults per stage`() {
        val custom = ShaderId(10)

        val key = keyOf(Material(shaders = mutableMapOf(ShaderType.FRAGMENT to custom)))

        assertEquals(defaults3D.vertex, key.vertexShader)
        assertEquals(custom, key.fragmentShader)
    }

    @Test
    fun `layouts without defaults need shaders from the material`() {
        val unknownLayout = object : VertexLayout<TexturedVertex3> {
            override val byteSize = 0

            override fun offset(property: KProperty1<TexturedVertex3, *>) = 0

            override fun getAttributeDescriptions(binding: Int) = emptyList<AttributeDescription>()
        }

        assertFailsWith<IllegalArgumentException> { keyOf(Material(), unknownLayout) }

        val complete = Material(
            shaders = mutableMapOf(ShaderType.VERTEX to ShaderId(7), ShaderType.FRAGMENT to ShaderId(8)),
        )
        assertEquals(ShaderId(7), keyOf(complete, unknownLayout).vertexShader)
    }
}
