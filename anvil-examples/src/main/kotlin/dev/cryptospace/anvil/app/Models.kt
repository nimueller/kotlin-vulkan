package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.VulkanEngine

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f

fun main() {
    Models()
}

class Models {

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

            val modelName = "/models/viking-room.obj"
            val modelResourceStream = Models::class.java.getResourceAsStream(modelName)
                ?: error("Model resource $modelName not found")
            val mesh = engine.modelManager.loadModel(modelResourceStream)[0]

            engine.camera.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))
            engine.camera.movementEnabled = false
            engine.camera.rotationEnabled = false

//            val mesh = engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)

            MainLoop(engine).loop { deltaTime, glfw, renderingContext ->
                updateModelMatrix(deltaTime, mesh)

                renderingContext.drawMesh(mesh)

                if (glfw.isKeyPressed(Key.ESCAPE)) {
                    glfw.window.requestClose()
                }
            }
        }
    }

    private var rotationInDegrees: Double = 0.0

    private fun updateModelMatrix(deltaTime: DeltaTime, mesh: Mesh) {
        val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
        rotationInDegrees += deltaRotation
        rotationInDegrees %= 360.0
        mesh.modelMatrix = Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))
    }
}
