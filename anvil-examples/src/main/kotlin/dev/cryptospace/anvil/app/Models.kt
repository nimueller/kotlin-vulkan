package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.VulkanEngine

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f

fun main() {
    Models()
}

class Models {

    init {
        VulkanEngine().use { engine ->
            val imageName = "/textures/viking-room.png"
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

            MainLoop(engine).loop { deltaTime, glfw, renderingContext ->
                updateModelMatrix(deltaTime, mesh)

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
