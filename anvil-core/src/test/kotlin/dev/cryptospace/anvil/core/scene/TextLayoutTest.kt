package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.image.Font
import dev.cryptospace.anvil.core.math.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextLayoutTest {

    private val glyphA = Font.Glyph(
        atlasX0 = 0,
        atlasY0 = 0,
        atlasX1 = 10,
        atlasY1 = 20,
        xOffset = 1f,
        yOffset = -15f,
        xAdvance = 12f,
    )
    private val glyphB = glyphA.copy(atlasX0 = 10, atlasX1 = 18, xAdvance = 9f)
    private val space = Font.Glyph(0, 0, 0, 0, 0f, 0f, 5f)

    private val font = Font(
        textureId = TextureId(1),
        atlasWidth = 100,
        atlasHeight = 50,
        pixelHeight = 24f,
        ascent = 18f,
        lineHeight = 24f,
        glyphs = mapOf('A' to glyphA, 'B' to glyphB, ' ' to space, Font.FALLBACK_CHAR to glyphB),
    )

    @Test
    fun `empty text produces no geometry`() {
        val mesh = TextLayout.build(font, "")

        assertEquals(0, mesh.vertices.size)
        assertEquals(0, mesh.indices.size)
    }

    @Test
    fun `places a glyph relative to the baseline`() {
        val mesh = TextLayout.build(font, "A")

        // x = pen 0 + xOffset 1, y = ascent 18 + yOffset -15
        assertEquals(
            listOf(Vec2(1f, 3f), Vec2(11f, 3f), Vec2(11f, 23f), Vec2(1f, 23f)),
            mesh.vertices.map { vertex -> vertex.position },
        )
        assertEquals(listOf(0, 1, 2, 2, 3, 0), mesh.indices.toList())
    }

    @Test
    fun `texture coordinates are normalized atlas coordinates`() {
        val mesh = TextLayout.build(font, "B")

        assertEquals(
            listOf(Vec2(0.1f, 0f), Vec2(0.18f, 0f), Vec2(0.18f, 0.4f), Vec2(0.1f, 0.4f)),
            mesh.vertices.map { vertex -> vertex.texture },
        )
    }

    @Test
    fun `advances the pen and skips geometry for whitespace`() {
        val mesh = TextLayout.build(font, "A B")

        assertEquals(8, mesh.vertices.size)
        // Second glyph starts after A's advance (12) and the space's advance (5), plus its offset
        assertEquals(18f, mesh.vertices[4].position.x)
        assertEquals(listOf(4, 5, 6, 6, 7, 4), mesh.indices.drop(6))
    }

    @Test
    fun `line breaks reset the pen and move down by the line height`() {
        val mesh = TextLayout.build(font, "A\nA")

        assertEquals(mesh.vertices[0].position.x, mesh.vertices[4].position.x)
        assertEquals(font.lineHeight, mesh.vertices[4].position.y - mesh.vertices[0].position.y)
    }

    @Test
    fun `unknown characters use the fallback glyph`() {
        val mesh = TextLayout.build(font, "é")

        assertEquals(TextLayout.build(font, "B").vertices.toList(), mesh.vertices.toList())
    }

    @Test
    fun `snaps glyph quads to whole pixels`() {
        val fractionalFont = font.copy(glyphs = mapOf('A' to glyphA.copy(xOffset = 0.4f, xAdvance = 10.3f)))

        val mesh = TextLayout.build(fractionalFont, "AA")

        assertTrue(mesh.vertices.all { vertex -> vertex.position.x % 1f == 0f && vertex.position.y % 1f == 0f })
    }
}
