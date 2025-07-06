package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

data class Window(
    val handle: Handle,
) : NativeResource() {

    fun shouldClose(): Boolean {
        check(!isDestroyed) { "Already destroyed" }
        return glfwWindowShouldClose(handle.value)
    }

    override fun destroy() {
        glfwDestroyWindow(handle.value)
    }
}
