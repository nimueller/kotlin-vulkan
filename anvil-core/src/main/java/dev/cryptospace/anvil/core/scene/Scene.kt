package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.window.Window

class Scene {

    private val _gameObjects = ArrayList<GameObject>()
    val gameObjects: List<GameObject>
        get() = _gameObjects

    fun gameObject(block: GameObject.() -> Unit): GameObject {
        val gameObject = GameObject()
        gameObject.block()
        _gameObjects.add(gameObject)
        return gameObject
    }

    fun remove(gameObject: GameObject): Boolean = _gameObjects.remove(gameObject)

    fun GameObject.renderComponent(block: RenderComponent.() -> Unit): RenderComponent {
        val renderComponent = RenderComponent()
        renderComponent.block()
        this.renderComponent = renderComponent
        return renderComponent
    }

    fun GameObject.onUpdate(block: (DeltaTime, Window) -> Unit) {
        onUpdate = block
    }

    internal fun update(deltaTime: DeltaTime, window: Window) {
        // Indexed loop: no iterator allocation, and no ConcurrentModificationException if a callback adds or
        // removes objects. Objects added during the update are first updated next frame.
        val count = _gameObjects.size
        var index = 0
        while (index < count && index < _gameObjects.size) {
            _gameObjects[index].onUpdate?.invoke(deltaTime, window)
            index++
        }
    }
}
