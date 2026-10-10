package dev.cryptospace.anvil.core.image

import dev.cryptospace.anvil.core.scene.TextureId

/**
 * A rasterized font whose glyphs are packed into a single texture atlas.
 *
 * All metrics are in pixels at the size the font was baked with. Y grows downwards.
 *
 * @property ascent Distance from the top of a line to its baseline
 * @property lineHeight Distance between the baselines of two consecutive lines
 */
data class Font(
    val textureId: TextureId,
    val atlasWidth: Int,
    val atlasHeight: Int,
    val pixelHeight: Float,
    val ascent: Float,
    val lineHeight: Float,
    val glyphs: Map<Char, Glyph>,
) {

    /**
     * Placement of a single character inside the atlas.
     *
     * @property atlasX0 Left edge of the glyph bitmap in the atlas
     * @property atlasY0 Top edge of the glyph bitmap in the atlas
     * @property atlasX1 Right edge (exclusive) of the glyph bitmap in the atlas
     * @property atlasY1 Bottom edge (exclusive) of the glyph bitmap in the atlas
     * @property xOffset Horizontal offset from the pen position to the left edge of the bitmap
     * @property yOffset Vertical offset from the baseline to the top edge of the bitmap
     * @property xAdvance How far the pen moves after this glyph
     */
    data class Glyph(
        val atlasX0: Int,
        val atlasY0: Int,
        val atlasX1: Int,
        val atlasY1: Int,
        val xOffset: Float,
        val yOffset: Float,
        val xAdvance: Float,
    ) {
        val width: Int
            get() = atlasX1 - atlasX0

        val height: Int
            get() = atlasY1 - atlasY0
    }

    companion object {
        const val FIRST_CHAR = ' '
        const val LAST_CHAR = '~'
        const val FALLBACK_CHAR = '?'
    }
}
