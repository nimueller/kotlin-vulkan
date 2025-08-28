package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import kotlin.reflect.KClass

data class VulkanMesh(
    val vertexType: KClass<out Vertex>,
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val indexType: Int,
)
