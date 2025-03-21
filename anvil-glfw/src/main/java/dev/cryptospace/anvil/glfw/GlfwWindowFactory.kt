package dev.cryptospace.anvil.glfw

import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.Window
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_OPENGL_API
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDefaultWindowHints
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.system.MemoryUtil.NULL

object GlfwWindowFactory {

    fun createWindow(renderingApi: RenderingApi): Window {
        val clientApi = when (renderingApi) {
            RenderingApi.OPENGL -> GLFW_OPENGL_API
            RenderingApi.VULKAN -> GLFW_NO_API
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, clientApi)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        val windowHandle = glfwCreateWindow(800, 600, "Hello World", NULL, NULL)
        check(windowHandle != NULL) { "Unable to create window" }
        return GlfwWindow(windowHandle)
    }

}
