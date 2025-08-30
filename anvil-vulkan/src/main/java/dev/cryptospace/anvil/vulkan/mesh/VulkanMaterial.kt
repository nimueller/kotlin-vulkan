package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType

data class VulkanMaterial(
    val texture: TextureId?,
    var shaders: Map<ShaderType, ShaderId>,
)
