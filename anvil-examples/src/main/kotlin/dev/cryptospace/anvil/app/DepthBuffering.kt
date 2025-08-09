package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.VulkanEngine

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f

fun main() {
    DepthBuffering()
}

class DepthBuffering {

    private val vertices = arrayOf(
        TexturedVertex2(
            position = Vec2(-0.5f, -0.5f),
            color = Vec3(1.0f, 0.0f, 0.0f),
            texture = Vec2(0f, 1f),
        ),
        TexturedVertex2(
            position = Vec2(0.5f, -0.5f),
            color = Vec3(0.0f, 1.0f, 0.0f),
            texture = Vec2(1f, 1f),
        ),
        TexturedVertex2(
            position = Vec2(0.5f, 0.5f),
            color = Vec3(0.0f, 0.0f, 1.0f),
            texture = Vec2(1f, 0f),
        ),
        TexturedVertex2(
            position = Vec2(-0.5f, 0.5f),
            color = Vec3(1.0f, 1.0f, 1.0f),
            texture = Vec2(0f, 0f),
        ),
    )

    private val indices = arrayOf(
        0U,
        1U,
        2U,
        2U,
        3U,
        0U,
    )

    init {
        VulkanEngine().use { engine ->
            val imageName = "/images/texture.jpg"
            val imageResourceStream = TexturedQuad::class.java.getResourceAsStream(imageName)
                ?: error("Image resource $imageName not found")
            engine.imageManager.loadImage(imageResourceStream)
            engine.camera.lookAt(Vec3(0f, 0f, 0f), Vec3(0f, 0f, -3f), Vec3(0f, 1f, 0f))
            engine.camera.movementEnabled = true
            engine.camera.rotationEnabled = true
            engine.window.captureCursor()

            val firstMesh = engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)
            val secondMesh = engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)

            MainLoop(engine).loop { _, glfw, renderingContext ->
                firstMesh.modelMatrix = Mat4.identity.translate(Vec3(0f, 0f, -3f))
                secondMesh.modelMatrix = Mat4.identity.translate(Vec3(0f, 0f, -4f))

                if (glfw.isKeyPressed(Key.ESCAPE)) {
                    glfw.window.requestClose()
                }
            }
        }
    }
}
