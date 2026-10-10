package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.FakeRenderingSystem
import dev.cryptospace.anvil.core.image.Font
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HUDTest {

    private val renderingSystem = FakeRenderingSystem()
    private val hud = HUD(renderingSystem)

    private val glyph = Font.Glyph(0, 0, 8, 8, 0f, -8f, 9f)
    private val font = Font(
        textureId = TextureId(1),
        atlasWidth = 64,
        atlasHeight = 64,
        pixelHeight = 10f,
        ascent = 8f,
        lineHeight = 10f,
        glyphs = ('0'..'9').associateWith { glyph } + mapOf(' ' to glyph.copy(atlasX1 = 0, atlasY1 = 0)),
    )

    @Test
    fun `creating a label uploads its text mesh`() {
        val label = hud.label("12", Vec2(5f, 5f), font)

        val mesh = assertNotNull(renderingSystem.meshes[assertNotNull(label.meshId)])
        assertEquals(TexturedVertex2, mesh.vertexLayout)
        assertEquals(8, mesh.vertices.size)
        assertEquals(listOf(label), hud.labels)
    }

    @Test
    fun `label material is blended, unculled and drawn without depth`() {
        val label = hud.label("1", Vec2.zero, font, Vec4(1f, 0f, 0f, 0.5f))

        val material = renderingSystem.getMaterial(label.materialId)
        assertEquals(font.textureId, material.texture)
        assertEquals(Vec4(1f, 0f, 0f, 0.5f), material.properties.albedo)
        assertEquals(BlendMode.ALPHA_BLEND, material.blendMode)
        assertEquals(CullMode.NONE, material.cullMode)
        assertFalse(material.depthTest)
        assertFalse(material.depthWrite)
    }

    @Test
    fun `labels with the same style share a material`() {
        val first = hud.label("1", Vec2.zero, font)
        val second = hud.label("2", Vec2.zero, font)
        val red = hud.label("3", Vec2.zero, font, Vec4(1f, 0f, 0f, 1f))

        assertEquals(first.materialId, second.materialId)
        assertNotEquals(first.materialId, red.materialId)
        assertEquals(2, renderingSystem.materialRegistry.size)
    }

    @Test
    fun `updating the text replaces the mesh and releases the old one`() {
        val label = hud.label("1", Vec2.zero, font)
        val oldMeshId = assertNotNull(label.meshId)

        hud.updateLabel(label, "12")

        assertEquals("12", label.text)
        assertEquals(listOf(oldMeshId), renderingSystem.removedMeshes)
        assertEquals(8, renderingSystem.meshes.getValue(assertNotNull(label.meshId)).vertices.size)
    }

    @Test
    fun `updating with identical text does not rebuild the mesh`() {
        val label = hud.label("1", Vec2.zero, font)
        val meshId = label.meshId

        hud.updateLabel(label, "1")

        assertEquals(meshId, label.meshId)
        assertTrue(renderingSystem.removedMeshes.isEmpty())
    }

    @Test
    fun `text without visible glyphs has no mesh`() {
        val label = hud.label("1", Vec2.zero, font)

        hud.updateLabel(label, " ")
        assertNull(label.meshId)

        hud.updateLabel(label, "")
        assertNull(label.meshId)
        assertEquals(1, renderingSystem.removedMeshes.size)
        assertTrue(renderingSystem.meshes.isEmpty())
    }

    @Test
    fun `removing labels releases their meshes`() {
        val first = hud.label("1", Vec2.zero, font)
        hud.label("2", Vec2.zero, font)

        hud.removeLabel(first)
        assertEquals(1, hud.labels.size)
        assertFailsWith<IllegalArgumentException> { hud.removeLabel(first) }

        hud.clear()
        assertTrue(hud.labels.isEmpty())
        assertTrue(renderingSystem.meshes.isEmpty())
    }
}
