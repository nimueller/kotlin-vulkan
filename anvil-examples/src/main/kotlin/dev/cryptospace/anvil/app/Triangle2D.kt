package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.VulkanEngine

private val vertices = listOf(
    TexturedVertex2(Vec2(-0.5f, 0.5f), Vec3(1.0f, 0.0f, 0.0f)),
    TexturedVertex2(Vec2(0f, -0.5f), Vec3(0.0f, 1.0f, 0.0f)),
    TexturedVertex2(Vec2(0.5f, 0.5f), Vec3(0.0f, 0.0f, 1.0f)),
)

private val indices = listOf(
    0.toShort(),
    1.toShort(),
    2.toShort(),
)

fun main() {
    VulkanEngine().use { engine ->
        val mesh = engine.renderingSystem.uploadMesh(vertices, indices)

        MainLoop(engine).loop { _, glfw, renderingContext ->
            renderingContext.drawMesh(mesh)

            if (glfw.isKeyPressed(Key.ESCAPE)) {
                glfw.window.requestClose()
            }
        }
    }
}
