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
        var frameCount = 0L
        val maxFrames = AppConfig.maxFrames

        while (!window.shouldClose() && (maxFrames == null || frameCount < maxFrames)) {
            val currentFrameTime = glfwGetTime()
            val deltaTime = DeltaTime(seconds = currentFrameTime - lastFrameTime)

            engine.frameStats.beginFrame()
            engine.update(deltaTime, logic)
            engine.frameStats.endFrame()
            lastFrameTime = currentFrameTime
            frameCount++
        }
    }
}
