package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwTerminate

data class Glfw(
    val window: Window,
) : NativeResource() {

    fun isKeyPressed(key: Key): Boolean = glfwGetKey(window.handle.value, key.code) == 1

    fun update() {
        glfwPollEvents()
        val pPositionX = DoubleArray(1)
        val pPositionY = DoubleArray(1)
        GLFW.glfwGetCursorPos(window.handle.value, pPositionX, pPositionY)

        window.previousCursorPosition = window.cursorPosition
        window.cursorPosition = Vec2(pPositionX[0].toFloat(), pPositionY[0].toFloat())
    }

    override fun destroy() {
        window.close()
        glfwTerminate()
    }

    override fun toString(): String = "Glfw"
}
