package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.vulkan.pipeline.Pipeline

data class VulkanMaterial(
    val pipeline: Pipeline,
    val texture: TextureId?,
)
