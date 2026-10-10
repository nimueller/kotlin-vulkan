package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.FakeRenderingSystem
import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderingSystemMaterialTest {

    private val renderingSystem = FakeRenderingSystem()

    @Test
    fun `accepts shaders used for the stage they were uploaded for`() {
        val vertex = renderingSystem.uploadShader(byteArrayOf(), ShaderType.VERTEX)
        val fragment = renderingSystem.uploadShader(byteArrayOf(), ShaderType.FRAGMENT)

        val materialId = renderingSystem.uploadMaterial(
            Material(shaders = mutableMapOf(ShaderType.VERTEX to vertex, ShaderType.FRAGMENT to fragment)),
        )

        assertEquals(vertex, renderingSystem.getMaterial(materialId).shaders[ShaderType.VERTEX])
    }

    @Test
    fun `rejects shaders used for the wrong stage`() {
        val fragment = renderingSystem.uploadShader(byteArrayOf(), ShaderType.FRAGMENT)

        assertFailsWith<IllegalArgumentException> {
            renderingSystem.uploadMaterial(Material(shaders = mutableMapOf(ShaderType.VERTEX to fragment)))
        }
    }

    @Test
    fun `rejects unknown shaders`() {
        assertFailsWith<IllegalArgumentException> {
            renderingSystem.uploadMaterial(Material(shaders = mutableMapOf(ShaderType.VERTEX to ShaderId(99))))
        }
    }

    @Test
    fun `invalid updates leave the previous material in place`() {
        val materialId = renderingSystem.uploadMaterial(Material(properties = MaterialProperties(albedo = Vec4.one)))

        assertFailsWith<IllegalArgumentException> {
            renderingSystem.updateMaterial(
                materialId,
                Material(shaders = mutableMapOf(ShaderType.FRAGMENT to ShaderId(99))),
            )
        }

        assertEquals(Vec4.one, renderingSystem.getMaterial(materialId).properties.albedo)
    }

    @Test
    fun `get modify update round trip`() {
        val materialId = renderingSystem.uploadMaterial(Material())

        val material = renderingSystem.getMaterial(materialId).apply { blendMode = BlendMode.ALPHA_BLEND }
        renderingSystem.updateMaterial(materialId, material)

        assertEquals(BlendMode.ALPHA_BLEND, renderingSystem.getMaterial(materialId).blendMode)
    }
}
