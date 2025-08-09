package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.VulkanEngine

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

fun main() {
    VulkanEngine().use { engine ->
        engine.renderingSystem.uploadMesh(TexturedVertex2::class, vertices, indices)

        MainLoop(engine).loop { _, glfw, _ ->
            if (glfw.isKeyPressed(Key.ESCAPE)) {
                glfw.window.requestClose()
            }
        }
    }
}
