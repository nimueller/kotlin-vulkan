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
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform

object WindowFactory {
    @JvmStatic
    private val logger = getLogger<WindowFactory>()

    init {
        setGlfwInitHints()

        GLFWErrorCallback.createPrint().set()

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

    fun createWindow(): Window {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)

        val windowHandle = glfwCreateWindow(800, 600, "Hello World", NULL, NULL)
        check(windowHandle != NULL) { "Unable to create window" }
        return Window(windowHandle)
    }

}
