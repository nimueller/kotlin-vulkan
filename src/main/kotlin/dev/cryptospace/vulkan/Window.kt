package dev.cryptospace.vulkan

import org.lwjgl.glfw.GLFW

data class Window(val handle: Long) {

    fun shouldClose(): Boolean {
        return GLFW.glfwWindowShouldClose(handle)
    }

}
