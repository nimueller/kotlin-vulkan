package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.image.Font
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import kotlin.math.floor

/**
 * Turns a string into a textured quad mesh using a [Font]'s glyph atlas.
 *
 * Positions are in pixels relative to the top-left corner of the text block, with Y growing downwards.
 */
object TextLayout {

    class Mesh(
        val vertices: Array<TexturedVertex2>,
        val indices: IntArray,
    )

    /**
     * Lays out [text], supporting `\n` line breaks. Characters missing from the font are replaced by
     * [Font.FALLBACK_CHAR], or skipped if the font lacks that as well. Whitespace produces no geometry.
     */
    fun build(font: Font, text: String): Mesh {
        val vertices = mutableListOf<TexturedVertex2>()
        val indices = mutableListOf<Int>()

        var penX = 0f
        var baseline = font.ascent

        for (char in text) {
            if (char == '\n') {
                penX = 0f
                baseline += font.lineHeight
                continue
            }

            val glyph = font.glyphs[char] ?: font.glyphs[Font.FALLBACK_CHAR] ?: continue

            if (glyph.width > 0 && glyph.height > 0) {
                // Snap to whole pixels so glyphs are sampled 1:1 and stay crisp
                val x0 = floor(penX + glyph.xOffset + 0.5f)
                val y0 = floor(baseline + glyph.yOffset + 0.5f)
                addQuad(vertices, indices, font, glyph, x0, y0)
            }

            penX += glyph.xAdvance
        }

        return Mesh(vertices.toTypedArray(), indices.toIntArray())
    }

    private fun addQuad(
        vertices: MutableList<TexturedVertex2>,
        indices: MutableList<Int>,
        font: Font,
        glyph: Font.Glyph,
        x0: Float,
        y0: Float,
    ) {
        val x1 = x0 + glyph.width
        val y1 = y0 + glyph.height
        val u0 = glyph.atlasX0.toFloat() / font.atlasWidth
        val v0 = glyph.atlasY0.toFloat() / font.atlasHeight
        val u1 = glyph.atlasX1.toFloat() / font.atlasWidth
        val v1 = glyph.atlasY1.toFloat() / font.atlasHeight

        val base = vertices.size
        vertices += TexturedVertex2(Vec2(x0, y0), Vec3.one, Vec2(u0, v0))
        vertices += TexturedVertex2(Vec2(x1, y0), Vec3.one, Vec2(u1, v0))
        vertices += TexturedVertex2(Vec2(x1, y1), Vec3.one, Vec2(u1, v1))
        vertices += TexturedVertex2(Vec2(x0, y1), Vec3.one, Vec2(u0, v1))

        indices += listOf(base, base + 1, base + 2, base + 2, base + 3, base)
    }
}
