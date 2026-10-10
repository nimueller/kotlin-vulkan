package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation

class VulkanMesh(
    val vertexLayout: VertexLayout<*>,
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val indexType: Int,
)
