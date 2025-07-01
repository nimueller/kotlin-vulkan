package dev.cryptospace.anvil.core.window

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

data class Window(val address: Address) : NativeResource() {

    fun shouldClose(): Boolean {
        check(!isDestroyed) { "Already destroyed" }
        return glfwWindowShouldClose(address.handle)
    }

    override fun destroy() {
        glfwDestroyWindow(address.handle)
    }
}
