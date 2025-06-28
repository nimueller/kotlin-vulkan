package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwTerminate

data class Glfw(val window: Window) : NativeResource() {

    fun update() {
        glfwPollEvents()
    }

    override fun destroy() {
        window.close()
        glfwTerminate()
    }

    override fun toString(): String {
        return "Glfw"
    }
}
