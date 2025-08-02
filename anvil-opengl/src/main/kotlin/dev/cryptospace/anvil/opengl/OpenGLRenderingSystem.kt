package dev.cryptospace.anvil.opengl

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import org.lwjgl.glfw.GLFW.glfwMakeContextCurrent
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import java.nio.ByteBuffer

class OpenGLRenderingSystem(
    private val glfw: Glfw,
) : RenderingSystem() {

    init {
        glfwMakeContextCurrent(glfw.window.handle.value)
        GL.createCapabilities()
    }

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Image {
        TODO("Not yet implemented")
    }

    override fun uploadMesh(vertex2: List<Vertex2>, indices: List<Short>): Mesh {
        TODO("Not yet implemented")
    }

    override fun drawFrame(callback: (RenderingContext) -> Unit) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)
        glfwSwapBuffers(glfw.window.handle.value)
    }

    override fun destroy() {
        // Nothing to do
    }
}
