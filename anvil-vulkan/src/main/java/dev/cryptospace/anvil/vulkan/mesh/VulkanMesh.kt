package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.pipeline.Pipeline

data class VulkanMesh(
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val indexType: Int,
    val pipeline: Pipeline,
)
