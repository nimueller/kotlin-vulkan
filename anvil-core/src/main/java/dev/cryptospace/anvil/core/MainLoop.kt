package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window
import org.lwjgl.glfw.GLFW.glfwGetTime

class MainLoop(
    private val engine: Engine,
) {

    fun loop(logic: (DeltaTime, Glfw, RenderingContext) -> Unit = { _, _, _ -> }) {
        loop(engine.window, logic)
    }

    private fun loop(window: Window, logic: (DeltaTime, Glfw, RenderingContext) -> Unit = { _, _, _ -> }) {
        var lastFrameTime = glfwGetTime()

        while (!window.shouldClose()) {
            val currentFrameTime = glfwGetTime()
            val deltaTime = DeltaTime(seconds = currentFrameTime - lastFrameTime)

            engine.update(deltaTime, logic)
            lastFrameTime = currentFrameTime
        }
    }
}
