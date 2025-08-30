package dev.cryptospace.anvil.opengl

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
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

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): TextureId {
        TODO("Not yet implemented")
    }

    override fun <V : Vertex> uploadMesh(
        vertexType: VertexLayout<V>,
        vertices: Array<V>,
        indices: Array<UInt>,
    ): MeshId {
        TODO("Not yet implemented")
    }

    override fun uploadMaterial(material: Material): MaterialId {
        TODO("Not yet implemented")
    }

    override fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId {
        TODO("Not yet implemented")
    }

    override fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT)
        glfwSwapBuffers(glfw.window.handle.value)
    }

    override fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4 =
        Mat4.perspectiveOpenGL(fov, aspect, near, far)

    override fun destroy() {
        // Nothing to do
    }
}
