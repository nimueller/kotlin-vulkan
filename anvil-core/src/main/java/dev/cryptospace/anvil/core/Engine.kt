package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.image.ImageManager
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.Camera
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.Face
import dev.cryptospace.anvil.core.scene.Face.Companion.toIndicesArray
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.Scene
import dev.cryptospace.anvil.core.scene.TextureId
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

    fun <V : Vertex> mesh(vertexLayout: VertexLayout<V>, vertices: Array<V>, faces: Array<Face>): MeshId = mesh(
        vertexLayout,
        vertices,
        faces.toIndicesArray(),
    )

    fun <V : Vertex> mesh(vertexLayout: VertexLayout<V>, vertices: Array<V>, indices: Array<UInt>): MeshId =
        renderingSystem.uploadMesh(
            vertexLayout,
            vertices,
            indices,
        )

    fun mesh(inputStream: InputStream): List<MeshId> = modelManager.loadModel(inputStream)

    fun texture(inputStream: InputStream): TextureId = imageManager.loadImage(inputStream)

    fun material(block: Material.() -> Unit): MaterialId = renderingSystem.uploadMaterial(Material().apply(block))

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
