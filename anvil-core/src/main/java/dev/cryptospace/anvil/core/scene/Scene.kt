package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.window.Window

class Scene {

    val gameObjects = mutableListOf<GameObject>()

    fun gameObject(block: GameObject.() -> Unit): GameObject {
        val gameObject = GameObject(
            visible = true,
            transform = Mat4.identity,
            renderComponent = null,
        )
        gameObject.block()
        gameObjects.add(gameObject)
        return gameObject
    }

    fun GameObject.renderComponent(block: RenderComponent.() -> Unit): RenderComponent {
        val renderComponent = RenderComponent(meshId = null, textureId = null)
        renderComponent.block()
        this.renderComponent = renderComponent
        return renderComponent
    }

    fun GameObject.onUpdate(block: (DeltaTime, Window) -> Unit) {
        onUpdate = block
    }

    internal fun update(deltaTime: DeltaTime, window: Window) {
        gameObjects.forEach { gameObject ->
            gameObject.onUpdate(deltaTime, window)
        }
    }
}
