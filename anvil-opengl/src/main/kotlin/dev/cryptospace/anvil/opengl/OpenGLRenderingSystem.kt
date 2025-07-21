package dev.cryptospace.anvil.opengl

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.window.Glfw
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor

class OpenGLRenderingSystem(
    private val glfw: Glfw,
) : RenderingSystem() {

    init {
        glfwMakeContextCurrent(glfw.window.handle.value)
        GL.createCapabilities()
    }

    override fun drawFrame() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)
        glfwSwapBuffers(glfw.window.handle.value)
    }

    override fun destroy() {
        // Nothing to do
    }
}
