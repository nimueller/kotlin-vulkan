package dev.cryptospace.anvil.glfw

import dev.cryptospace.anvil.core.Window
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

data class GlfwWindow(val handle: Long) : Window {

    override fun shouldClose(): Boolean {
        return glfwWindowShouldClose(handle)
    }

}
