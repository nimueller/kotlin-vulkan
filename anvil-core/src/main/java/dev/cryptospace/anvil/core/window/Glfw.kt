package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.system.MemoryUtil

object Glfw : NativeResource() {

    private val windows = mutableListOf<Window>()

    init {
        GlfwInit.init()
    }

    fun update() {
        glfwPollEvents()
    }

    fun createWindow(renderingSystem: RenderingSystem): Window {
        val clientApi = when (renderingSystem.renderingApi) {
            RenderingApi.OPENGL -> GLFW_OPENGL_API
            RenderingApi.VULKAN -> GLFW_NO_API
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, clientApi)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        val windowHandle = glfwCreateWindow(800, 600, "Hello World", MemoryUtil.NULL, MemoryUtil.NULL)
        check(windowHandle != MemoryUtil.NULL) { "Unable to create window" }
        val window = Window(windowHandle)
        renderingSystem.initWindow(window)
        windows.add(window)
        return window
    }

    override fun destroy() {
        for (window in windows) {
            window.close()
        }

        glfwTerminate()
    }

    override fun toString(): String {
        return "Glfw"
    }

}
