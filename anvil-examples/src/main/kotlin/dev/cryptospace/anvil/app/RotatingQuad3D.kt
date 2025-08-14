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

private const val ROTATION_DEGREES_PER_SECOND: Float = 90f

fun main() {
    RotatingQuad3D()
}

class RotatingQuad3D {

    private val vertices = arrayOf(
        TexturedVertex2(Vec2(-0.5f, -0.5f), Vec3(1.0f, 0.0f, 0.0f)),
        TexturedVertex2(Vec2(0.5f, -0.5f), Vec3(0.0f, 1.0f, 0.0f)),
        TexturedVertex2(Vec2(0.5f, 0.5f), Vec3(0.0f, 0.0f, 1.0f)),
        TexturedVertex2(Vec2(-0.5f, 0.5f), Vec3(1.0f, 1.0f, 1.0f)),
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
            val mesh = engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)

            MainLoop(engine).loop { deltaTime, glfw, _ ->
//                updateModelMatrix(deltaTime, mesh)

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
