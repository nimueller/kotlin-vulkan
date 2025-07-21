package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window

class MainLoop(
    private val engine: Engine,
) {

    fun loop(logic: (Glfw, RenderingSystem) -> Unit) {
        loop(engine.window, logic)
    }

    private fun loop(window: Window, logic: (Glfw, RenderingSystem) -> Unit = { _, _ -> }) {
        var lastFrameTime = System.nanoTime()

        while (!window.shouldClose()) {
            val currentFrameTime = System.nanoTime()
            val timeSinceLastFrame = currentFrameTime - lastFrameTime
            val fps = 1_000_000_000.0 / timeSinceLastFrame.toDouble()

//            println()
//            print("Time since last frame: $timeSinceLastFrame ns ")
//            print("FPS: $fps")

            engine.update(logic)
            lastFrameTime = System.nanoTime()
        }
    }
}
