package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.VulkanEngine

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f

fun main() {
    TexturedQuad()
}

class TexturedQuad {

    private val vertices = listOf(
        TexturedVertex2(
            position = Vec2(-0.5f, -0.5f),
            color = Vec3(1.0f, 0.0f, 0.0f),
            texture = Vec2(1f, 0f),
        ),
        TexturedVertex2(
            position = Vec2(0.5f, -0.5f),
            color = Vec3(0.0f, 1.0f, 0.0f),
            texture = Vec2(0f, 0f),
        ),
        TexturedVertex2(position = Vec2(0.5f, 0.5f), color = Vec3(0.0f, 0.0f, 1.0f), texture = Vec2(0f, 1f)),
        TexturedVertex2(
            position = Vec2(-0.5f, 0.5f),
            color = Vec3(1.0f, 1.0f, 1.0f),
            texture = Vec2(1f, 1f),
        ),
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
            val imageName = "/images/texture.jpg"
            val imageResourceStream = TexturedQuad::class.java.getResourceAsStream(imageName)
                ?: error("Image resource $imageName not found")
            engine.imageManager.loadImage(imageResourceStream)

            val mesh = engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)

            MainLoop(engine).loop { deltaTime, glfw, renderingContext ->
                updateCamera(renderingContext)
                updateModelMatrix(deltaTime, mesh)

                renderingContext.drawMesh(mesh)

                if (glfw.isKeyPressed(Key.ESCAPE)) {
                    glfw.window.requestClose()
                }
            }
        }
    }

    private fun updateCamera(renderingContext: RenderingContext) {
        val view = Mat4.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))
        val projection = Mat4.perspectiveVulkan(
            45f,
            renderingContext.width.toFloat() / renderingContext.height.toFloat(),
            0.1f,
            100f,
        )

        val camera = renderingContext.camera
        camera.projectionMatrix = projection
        camera.viewMatrix = view
    }

    private var rotationInDegrees: Double = 0.0

    private fun updateModelMatrix(deltaTime: DeltaTime, mesh: Mesh) {
        val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
        rotationInDegrees += deltaRotation
        rotationInDegrees %= 360.0
        mesh.modelMatrix = Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))
    }
}
