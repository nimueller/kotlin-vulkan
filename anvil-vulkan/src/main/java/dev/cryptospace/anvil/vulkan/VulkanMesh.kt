package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline

data class VulkanMesh(
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val graphicsPipeline: GraphicsPipeline,
) : Mesh
