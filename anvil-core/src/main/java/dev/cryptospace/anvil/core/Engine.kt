package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.window.Glfw

class Engine(
    renderingSystemCreator: () -> RenderingSystem
) : AutoCloseable {

    private val renderingSystem: RenderingSystem = renderingSystemCreator()
    val mainWindow = Glfw.createWindow(renderingSystem)

    fun update() {
        Glfw.update()
    }

    override fun close() {
        renderingSystem.close()
        Glfw.close()
    }

}
