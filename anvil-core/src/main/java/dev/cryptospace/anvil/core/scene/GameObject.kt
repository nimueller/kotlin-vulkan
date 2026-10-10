package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.window.Window

/**
 * An object in the scene. Compared by identity, so it can safely be used as a map key while it changes.
 */
class GameObject {

    var visible: Boolean = true

    val transform: Transform = Transform()

    var renderComponent: RenderComponent? = null

    /** Called once per frame before rendering, or never if null. */
    var onUpdate: ((DeltaTime, Window) -> Unit)? = null
}
