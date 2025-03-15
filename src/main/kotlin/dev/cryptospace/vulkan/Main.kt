package dev.cryptospace.vulkan

import dev.cryptospace.vulkan.utils.getLogger
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwPlatformSupported
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil.NULL

fun main() {
    Main.start()
}

object Main {
    @JvmStatic
    private val logger = getLogger<Main>()

    fun start() {
        if (useWayland()) {
            logger.info("Using Wayland")
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
        } else if (glfwPlatformSupported(GLFW_PLATFORM_X11)) {
            logger.info("Using X11")
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11)
        }

        GLFWErrorCallback.createPrint().set()

        val initSuccessful = glfwInit()
        check(initSuccessful) { "Unable to initialize GLFW" }

        try {
            val window = createWindow()
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

    private fun useWayland(): Boolean {
        if (AppConfig.preferX11) {
            logger.info("Explicitly preferring X11 over Wayland according to configuration")
            return false
        }

        val waylandSupported = glfwPlatformSupported(GLFW_PLATFORM_WAYLAND)

        if (!waylandSupported) {
            logger.info("Wayland not supported, falling back to X11")
        }

        return waylandSupported
    }

    private fun createWindow(): Window {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)

        val windowHandle = glfwCreateWindow(800, 600, "Hello World", NULL, NULL)
        check(windowHandle != NULL) { "Unable to create window" }
        return Window(windowHandle)
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
