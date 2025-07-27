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

private var x = 0f
private var z = 3f

fun main() {
    VulkanEngine().use { engine ->
        val mesh = engine.renderingSystem.uploadMesh(vertices, indices)

        MainLoop(engine).loop { deltaTime, glfw, renderingContext ->
            updateUniformBufferObject(deltaTime, renderingContext)
            renderingContext.drawMesh(mesh)

            if (glfw.isKeyPressed(Key.A)) {
                x -= 0.01f
            }

            if (glfw.isKeyPressed(Key.D)) {
                x += 0.01f
            }

            if (glfw.isKeyPressed(Key.S)) {
                z += 0.01f
            }

            if (glfw.isKeyPressed(Key.W)) {
                z -= 0.01f
            }

            if (glfw.isKeyPressed(Key.ESCAPE)) {
                glfw.window.requestClose()
            }
        }
    }
}

private const val ROTATION_DEGREES_PER_SECOND: Float = 90f
private var rotationInDegrees: Double = 0.0

fun updateUniformBufferObject(deltaTime: DeltaTime, renderingContext: RenderingContext) {
    val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
    print("\r ${(deltaTime.seconds * 1_000_000_000).toInt()}\t $deltaRotation")
    rotationInDegrees += deltaRotation.toLong()
    rotationInDegrees %= 360.0

    val model = Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))
    val view = Mat4.lookAt(Vec3(x, 0f, z), Vec3(0f, 0f, -3f), Vec3(0f, 1f, 0f))
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
