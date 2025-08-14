package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.math.Mat4

@Deprecated("Use Scene.addMesh instead")
data class Mesh(
    var visible: Boolean,
    var modelMatrix: Mat4,
)
