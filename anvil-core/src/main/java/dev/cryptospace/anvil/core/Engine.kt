package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.ImageManager
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.GlfwFactory

open class Engine(
    renderingApi: RenderingApi,
    renderingSystemCreator: (Glfw) -> RenderingSystem,
) : NativeResource() {

    val glfw = GlfwFactory.create(renderingApi)
    val window = glfw.window
    val renderingSystem: RenderingSystem = renderingSystemCreator(glfw)
    val imageManager: ImageManager = ImageManager(renderingSystem)

    internal fun update(deltaTime: DeltaTime, logic: (DeltaTime, Glfw, RenderingContext) -> Unit) {
        glfw.update()
        renderingSystem.drawFrame { renderingContext ->
            logic(deltaTime, glfw, renderingContext)
        }
    }

    fun loadImage(resourcePath: String) {
    }

    override fun destroy() {
        renderingSystem.close()
        glfw.close()
    }

    override fun toString(): String = "Engine"
}
