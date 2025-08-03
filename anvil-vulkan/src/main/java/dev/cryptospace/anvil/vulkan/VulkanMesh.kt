package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline

data class VulkanMesh(
    override var modelMatrix: Mat4 = Mat4.identity,
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val graphicsPipeline: GraphicsPipeline,
) : Mesh
