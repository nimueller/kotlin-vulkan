package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.window.Window

data class GameObject(
    var visible: Boolean = true,
    var transform: Mat4 = Mat4.identity,
    var renderComponent: RenderComponent? = null,
    var onUpdate: (DeltaTime, Window) -> Unit = { _, _ -> },
)
