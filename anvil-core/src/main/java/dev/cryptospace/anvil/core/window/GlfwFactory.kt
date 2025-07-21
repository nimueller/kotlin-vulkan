package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.Handle
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwPlatformSupported
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform

object GlfwFactory {

    @JvmStatic
    private val logger = logger<GlfwFactory>()

    init {
        setGlfwInitHints()

        GLFWErrorCallback.create { error, description ->
            logger.error("GLFW Error $error: $description")
        }.set()

        val initSuccessful = glfwInit()
        check(initSuccessful) { "Unable to initialize GLFW" }
    }

    private fun setGlfwInitHints() {
        val platform = Platform.get()

        if (platform != Platform.LINUX) {
            logger.info("Not setting any GLFW init hints because we are not running on Linux, letting GLFW decide")
            return
        }

        if (useWayland()) {
            logger.info("Using Wayland")
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
        } else if (glfwPlatformSupported(GLFW_PLATFORM_X11)) {
            logger.info("Using X11")
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11)
        } else {
            logger.warn("Neither Wayland nor X11 supported, letting GLFW decide. Are you running in a time machine?")
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

    private fun createWindow(renderingApi: RenderingApi): Window {
        val clientApi = when (renderingApi) {
            RenderingApi.OPENGL -> GLFW_OPENGL_API
            RenderingApi.VULKAN -> GLFW_NO_API
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, clientApi)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        val windowHandle = glfwCreateWindow(800, 600, "Hello World", MemoryUtil.NULL, MemoryUtil.NULL)
        glfwShowWindow(windowHandle)
        check(windowHandle != MemoryUtil.NULL) { "Unable to create window" }
        return Window(Handle(windowHandle))
    }

    fun create(renderingApi: RenderingApi): Glfw {
        val window = createWindow(renderingApi)
        return Glfw(window)
    }
}
