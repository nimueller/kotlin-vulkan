package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.GlfwFactory

class Engine(
    renderingApi: RenderingApi,
    renderingSystemCreator: (Glfw) -> RenderingSystem,
) : NativeResource() {

    val glfw = GlfwFactory.create(renderingApi)
    val window = glfw.window

    private val renderingSystem: RenderingSystem = renderingSystemCreator(glfw)

    fun update() {
        glfw.update()
    }

    override fun destroy() {
        renderingSystem.close()
        glfw.close()
    }

    override fun toString(): String {
        return "Engine"
    }
}
