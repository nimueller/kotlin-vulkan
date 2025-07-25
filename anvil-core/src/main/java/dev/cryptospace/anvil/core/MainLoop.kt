package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window

class MainLoop(
    private val engine: Engine,
) {

    fun loop(logic: (Long, Glfw, RenderingContext) -> Unit) {
        loop(engine.window, logic)
    }

    private fun loop(window: Window, logic: (Long, Glfw, RenderingContext) -> Unit = { _, _, _ -> }) {
        var lastFrameTime = System.nanoTime()

        while (!window.shouldClose()) {
            val currentFrameTime = System.nanoTime()
            val timeSinceLastFrame = currentFrameTime - lastFrameTime

            engine.update(timeSinceLastFrame, logic)
            lastFrameTime = System.nanoTime()
        }
    }
}
