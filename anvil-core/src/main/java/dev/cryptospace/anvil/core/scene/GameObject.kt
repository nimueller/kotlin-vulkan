package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.window.Window

data class GameObject(
    var visible: Boolean,
    var transform: Mat4,
    var renderComponent: RenderComponent? = null,
    var onUpdate: (DeltaTime, Window) -> Unit = { _, _ -> },
)
