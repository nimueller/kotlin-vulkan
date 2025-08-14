package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.ImageManager
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.Camera
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.Scene
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.GlfwFactory
import java.io.InputStream

open class Engine(
    renderingApi: RenderingApi,
    renderingSystemCreator: (Glfw) -> RenderingSystem,
) : NativeResource() {

    val glfw = GlfwFactory.create(renderingApi)
    val window = glfw.window
    val renderingSystem: RenderingSystem = renderingSystemCreator(glfw)
    val imageManager: ImageManager = ImageManager(renderingSystem)
    val modelManager: ModelManager = ModelManager(renderingSystem)
    val camera: Camera = Camera(renderingSystem)
    var scene: Scene = Scene()

    fun scene(block: Scene.() -> Unit): Scene {
        val scene = Scene()
        scene.block()
        this.scene = scene
        return scene
    }

    fun mesh(inputStream: InputStream): List<MeshId> = modelManager.loadModel(inputStream)

    fun material(inputStream: InputStream) = imageManager.loadImage(inputStream)

    internal fun update(deltaTime: DeltaTime, logic: (DeltaTime, Glfw, RenderingContext) -> Unit) {
        glfw.update()
        scene.update(deltaTime, window)
        renderingSystem.drawFrame(this) { renderingContext ->
            camera.update(window, renderingContext, deltaTime)
            logic(deltaTime, glfw, renderingContext)
        }
    }

    override fun destroy() {
        renderingSystem.close()
        glfw.close()
    }

    override fun toString(): String = "Engine"
}
