package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

/**
 * Represents a window in the application using GLFW.
 *
 * @property handle The native GLFW window handle wrapper
 */
data class Window(
    val handle: Handle,
) : NativeResource() {

    /**
     * Checks if the window should close.
     *
     * @throws IllegalStateException if the window has already been destroyed
     * @return true if the window should close, false otherwise
     */
    fun shouldClose(): Boolean {
        check(!isDestroyed) { "Already destroyed" }
        return glfwWindowShouldClose(handle.value)
    }

    /**
     * Requests the window to close by setting its close flag.
     *
     * @throws IllegalStateException if the window has already been destroyed
     */
    fun requestClose() {
        check(!isDestroyed) { "Already destroyed" }
        glfwSetWindowShouldClose(handle.value, true)
    }

    override fun destroy() {
        glfwDestroyWindow(handle.value)
    }
}
