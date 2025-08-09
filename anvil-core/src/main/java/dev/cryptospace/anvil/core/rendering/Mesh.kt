package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.math.Mat4

data class Mesh(
    var visible: Boolean,
    var modelMatrix: Mat4,
)
