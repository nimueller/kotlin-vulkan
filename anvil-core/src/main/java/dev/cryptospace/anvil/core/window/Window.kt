package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_PRESS

/**
 * Represents a window in the application using GLFW.
 *
 * @property handle The native GLFW window handle wrapper
 */
data class Window(
    val handle: Handle,
) : NativeResource() {

    var previousCursorPosition: Vec2 = Vec2(0f, 0f)
        internal set

    var cursorPosition: Vec2 = Vec2(0f, 0f)
        internal set

    fun captureCursor() {
        GLFW.glfwSetInputMode(handle.value, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
    }

    /**
     * Checks if a specific key is currently being pressed.
     *
     * @param key The key to check
     * @return true if the key is pressed, false otherwise
     * @throws IllegalStateException if the window has already been destroyed
     */
    fun isKeyPressed(key: Key): Boolean = GLFW.glfwGetKey(handle.value, key.code) == GLFW_PRESS

    /**
     * Checks if the window should close.
     *
     * @throws IllegalStateException if the window has already been destroyed
     * @return true if the window should close, false otherwise
     */
    fun shouldClose(): Boolean {
        check(!isDestroyed) { "Already destroyed" }
        return GLFW.glfwWindowShouldClose(handle.value)
    }

    /**
     * Requests the window to close by setting its close flag.
     *
     * @throws IllegalStateException if the window has already been destroyed
     */
    fun requestClose() {
        check(!isDestroyed) { "Already destroyed" }
        GLFW.glfwSetWindowShouldClose(handle.value, true)
    }

    override fun destroy() {
        GLFW.glfwDestroyWindow(handle.value)
    }
}
