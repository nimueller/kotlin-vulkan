package dev.cryptospace.anvil.core.image

import dev.cryptospace.anvil.core.RenderingSystem
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.InputStream
import java.nio.ByteBuffer

private const val MIN_ATLAS_SIZE = 256
private const val MAX_ATLAS_SIZE = 4096
private const val RGBA_CHANNELS = 4
private const val OPAQUE_WHITE: Byte = -1

/**
 * Rasterizes TrueType fonts into texture atlases using stb_truetype.
 */
class FontManager(
    private val renderingSystem: RenderingSystem,
) {

    /**
     * Loads a TrueType font and bakes the printable ASCII range into a texture atlas.
     *
     * @param inputStream TTF file contents
     * @param pixelHeight Height of a line of text in pixels
     */
    fun loadFont(inputStream: InputStream, pixelHeight: Float): Font {
        require(pixelHeight > 0f) { "Font pixel height must be positive, was $pixelHeight" }

        val bytes = inputStream.readAllBytes()
        val fontData = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()

        try {
            return bakeFont(fontData, pixelHeight)
        } finally {
            MemoryUtil.memFree(fontData)
        }
    }

    private fun bakeFont(fontData: ByteBuffer, pixelHeight: Float): Font = MemoryStack.stackPush().use { stack ->
        val fontInfo = STBTTFontinfo.malloc(stack)
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontData)) {
            throw FailedToLoadFontException("Not a valid TrueType font")
        }

        val scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixelHeight)
        val ascent = stack.mallocInt(1)
        val descent = stack.mallocInt(1)
        val lineGap = stack.mallocInt(1)
        STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap)

        val charCount = Font.LAST_CHAR - Font.FIRST_CHAR + 1
        val bakedChars = STBTTBakedChar.malloc(charCount, stack)
        val (atlasSize, coverage) = bakeAtlas(fontData, pixelHeight, bakedChars)

        try {
            val textureId = uploadAtlas(coverage, atlasSize)
            val glyphs = (0 until charCount).associate { index ->
                val bakedChar = bakedChars[index]
                (Font.FIRST_CHAR + index) to Font.Glyph(
                    atlasX0 = bakedChar.x0().toInt(),
                    atlasY0 = bakedChar.y0().toInt(),
                    atlasX1 = bakedChar.x1().toInt(),
                    atlasY1 = bakedChar.y1().toInt(),
                    xOffset = bakedChar.xoff(),
                    yOffset = bakedChar.yoff(),
                    xAdvance = bakedChar.xadvance(),
                )
            }

            Font(
                textureId = textureId,
                atlasWidth = atlasSize,
                atlasHeight = atlasSize,
                pixelHeight = pixelHeight,
                ascent = ascent[0] * scale,
                lineHeight = (ascent[0] - descent[0] + lineGap[0]) * scale,
                glyphs = glyphs,
            )
        } finally {
            MemoryUtil.memFree(coverage)
        }
    }

    /**
     * Bakes all glyphs into the smallest square atlas they fit in.
     * Returns the atlas size and its single-channel coverage bitmap, which the caller must free.
     */
    private fun bakeAtlas(
        fontData: ByteBuffer,
        pixelHeight: Float,
        bakedChars: STBTTBakedChar.Buffer,
    ): Pair<Int, ByteBuffer> {
        var size = MIN_ATLAS_SIZE

        while (size <= MAX_ATLAS_SIZE) {
            val coverage = MemoryUtil.memCalloc(size * size)
            val result = STBTruetype.stbtt_BakeFontBitmap(
                fontData,
                pixelHeight,
                coverage,
                size,
                size,
                Font.FIRST_CHAR.code,
                bakedChars,
            )

            // A non-positive result means not all glyphs fit into the atlas
            if (result > 0) {
                return size to coverage
            }

            MemoryUtil.memFree(coverage)
            size *= 2
        }

        throw FailedToLoadFontException("Font at $pixelHeight px does not fit into a $MAX_ATLAS_SIZE px atlas")
    }

    /**
     * Uploads the coverage bitmap as white RGBA texels whose alpha is the glyph coverage,
     * so the text color can be applied through the material's albedo.
     */
    private fun uploadAtlas(coverage: ByteBuffer, size: Int) = MemoryUtil.memAlloc(size * size * RGBA_CHANNELS)
        .let { rgba ->
            try {
                for (index in 0 until size * size) {
                    rgba.put(OPAQUE_WHITE).put(OPAQUE_WHITE).put(OPAQUE_WHITE).put(coverage[index])
                }
                rgba.flip()
                renderingSystem.uploadImage(rgba.remaining(), size, size, rgba)
            } finally {
                MemoryUtil.memFree(rgba)
            }
        }
}
