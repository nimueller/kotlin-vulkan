package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType

data class Material(
    val texture: Texture,
    val shaders: Map<ShaderType, ShaderId>,
)
