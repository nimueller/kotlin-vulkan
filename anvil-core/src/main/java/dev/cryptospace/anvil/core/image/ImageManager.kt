package dev.cryptospace.anvil.core.image

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.scene.TextureId
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.STBI_rgb_alpha
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer

class ImageManager(
    private val renderingSystem: RenderingSystem,
) {

    fun loadImage(inputStream: InputStream): TextureId {
        val bytes = inputStream.readAllBytes()
        val textureByteBuffer = MemoryUtil.memAlloc(bytes.size)
            .put(bytes, 0, bytes.size)
            .flip()

        try {
            return loadImage(textureByteBuffer)
        } finally {
            MemoryUtil.memFree(textureByteBuffer)
        }
    }

    private fun loadImage(imageBytes: ByteBuffer): TextureId = MemoryStack.stackPush().use { stack ->
        val widthBuffer: IntBuffer = stack.ints(0)
        val heightBuffer: IntBuffer = stack.ints(0)
        val channelsInFileBuffer: IntBuffer = stack.ints(0)
        val channelCount = STBI_rgb_alpha

        val data: ByteBuffer? = STBImage.stbi_load_from_memory(
            imageBytes,
            widthBuffer,
            heightBuffer,
            channelsInFileBuffer,
            channelCount,
        )

        check(data != null) { throw FailedToLoadImageException(STBImage.stbi_failure_reason()) }

        try {
            val width = widthBuffer[0]
            val height = heightBuffer[0]
            val imageSize = width * height * channelCount
            renderingSystem.uploadImage(imageSize, width, height, data)
        } finally {
            STBImage.stbi_image_free(data)
        }
    }
}
