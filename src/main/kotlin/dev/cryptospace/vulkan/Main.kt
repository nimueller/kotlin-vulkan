package dev.cryptospace.vulkan

import dev.cryptospace.vulkan.core.Vulkan
import dev.cryptospace.vulkan.utils.getLogger
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwTerminate

fun main() {
    Main.start()
}

object Main {
    @JvmStatic
    private val logger = getLogger<Main>()

    fun start() {
        try {
            val window = WindowFactory.createWindow()
            val vulkan = Vulkan(
                useValidationLayers = AppConfig.useValidationLayers,
                validationLayerNames = AppConfig.validationLayers
            )

            try {
                glfwShowWindow(window.handle)
//                loop(window)
            } finally {
                vulkan.close()
            }
        } finally {
            glfwTerminate()
        }
    }

    private fun loop(window: Window) {
        var lastFrameTime = System.nanoTime()

        while (!window.shouldClose()) {
            val currentFrameTime = System.nanoTime()
            val timeSinceLastFrame = currentFrameTime - lastFrameTime
            val fps = 1_000_000_000.0 / timeSinceLastFrame.toDouble()
            println()
            print("Time since last frame: $timeSinceLastFrame ns ")
            print("FPS: $fps")

            glfwPollEvents()
            lastFrameTime = System.nanoTime()
        }
    }
}
