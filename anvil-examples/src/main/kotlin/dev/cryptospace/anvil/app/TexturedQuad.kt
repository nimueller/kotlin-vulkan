package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.native.UniformBufferObject
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.VulkanEngine
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImage.STBI_rgb_alpha
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer
import java.nio.IntBuffer

private const val ROTATION_DEGREES_PER_SECOND: Float = 90f

fun main() {
    TexturedQuad()
}

class TexturedQuad {

    private val vertices = listOf(
        Vertex2(Vec2(-0.5f, -0.5f), Vec3(1.0f, 0.0f, 0.0f)),
        Vertex2(Vec2(0.5f, -0.5f), Vec3(0.0f, 1.0f, 0.0f)),
        Vertex2(Vec2(0.5f, 0.5f), Vec3(0.0f, 0.0f, 1.0f)),
        Vertex2(Vec2(-0.5f, 0.5f), Vec3(1.0f, 1.0f, 1.0f)),
    )

    private val indices = listOf(
        0.toShort(),
        1.toShort(),
        2.toShort(),
        2.toShort(),
        3.toShort(),
        0.toShort(),
    )

    init {
        VulkanEngine().use { engine ->
            MemoryStack.stackPush().use { stack ->
                val imageName = "/textures/texture.jpg"
                val textureBytes = TexturedQuad::class.java.getResourceAsStream(imageName).readAllBytes()
                val textureByteBuffer = stack.malloc(textureBytes.size)
                    .put(textureBytes, 0, textureBytes.size)
                    .flip()

                val widthBuffer: IntBuffer = stack.ints(0)
                val heightBuffer: IntBuffer = stack.ints(0)
                val channelsInFileBuffer: IntBuffer = stack.ints(0)
                val data: ByteBuffer? = STBImage.stbi_load_from_memory(
                    textureByteBuffer,
                    widthBuffer,
                    heightBuffer,
                    channelsInFileBuffer,
                    STBI_rgb_alpha,
                )

                check(data != null) { "Failed to load texture image $imageName: ${STBImage.stbi_failure_reason()}" }
                val imageSize = widthBuffer[0] * heightBuffer[0] * 4
                val textureImage = engine.renderingSystem.createTextureImage(
                    imageSize,
                    data,
                    widthBuffer[0],
                    heightBuffer[0],
                )
            }

            val mesh = engine.renderingSystem.uploadMesh(vertices, indices)

            MainLoop(engine).loop { deltaTime, glfw, renderingContext ->
                updateUniformBufferObject(deltaTime, renderingContext)
                renderingContext.drawMesh(mesh)

                if (glfw.isKeyPressed(Key.ESCAPE)) {
                    glfw.window.requestClose()
                }
            }
        }
    }

    private var rotationInDegrees: Double = 0.0

    private fun updateUniformBufferObject(deltaTime: DeltaTime, renderingContext: RenderingContext) {
        val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
        rotationInDegrees += deltaRotation
        rotationInDegrees %= 360.0

        val model =
            Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))
        val view = Mat4.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))
        val projection = Mat4.perspectiveVulkan(
            45f,
            renderingContext.width.toFloat() / renderingContext.height.toFloat(),
            0.1f,
            100f,
        )

        renderingContext.uniformBufferObject = UniformBufferObject(
            model,
            view,
            projection,
        )
    }
}
