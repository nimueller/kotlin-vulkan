package dev.cryptospace.anvil.core

class Engine(
    renderingApi: RenderingApi,
    windowSystemCreator: () -> WindowSystem,
    renderingSystemCreator: () -> RenderingSystem
) : AutoCloseable {

    private val windowSystem: WindowSystem = windowSystemCreator().also { it.renderingApi = renderingApi }
    private val renderingSystem: RenderingSystem = renderingSystemCreator()
    val mainWindow = windowSystem.createWindow()

    fun update() {
        windowSystem.update()
    }

    override fun close() {
        windowSystem.close()
        renderingSystem.close()
    }

}
