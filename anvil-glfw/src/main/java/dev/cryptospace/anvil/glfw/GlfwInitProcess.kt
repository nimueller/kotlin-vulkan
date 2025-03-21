package dev.cryptospace.anvil.glfw

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.logger
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WAYLAND
import org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwInitHint
import org.lwjgl.glfw.GLFW.glfwPlatformSupported
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.Platform

object GlfwInitProcess {

    @JvmStatic
    private val logger = logger<GlfwInitProcess>()

    fun init() {
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

}
