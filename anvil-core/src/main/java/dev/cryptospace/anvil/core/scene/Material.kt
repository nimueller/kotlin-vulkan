package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType

data class Material(
    var texture: TextureId? = null,
    var shaders: MutableMap<ShaderType, ShaderId> = mutableMapOf(),
)
