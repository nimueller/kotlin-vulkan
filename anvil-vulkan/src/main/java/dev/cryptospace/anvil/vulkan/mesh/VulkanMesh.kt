package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10

data class VulkanMesh(
    val mesh: Mesh,
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val indexType: Int,
    val graphicsPipeline: GraphicsPipeline,
) {

    fun draw(stack: MemoryStack, commandBuffer: CommandBuffer) {
        if (!mesh.visible) {
            return
        }

        val vertexBuffers = stack.longs(vertexBufferAllocation.buffer.value)
        val offsets = stack.longs(0L)

        VK10.vkCmdPushConstants(
            commandBuffer.handle,
            graphicsPipeline.pipelineLayoutHandle.value,
            VK10.VK_SHADER_STAGE_VERTEX_BIT,
            0,
            mesh.modelMatrix.toByteBuffer(stack),
        )
        VK10.vkCmdBindVertexBuffers(commandBuffer.handle, 0, vertexBuffers, offsets)
        VK10.vkCmdBindIndexBuffer(
            commandBuffer.handle,
            indexBufferAllocation.buffer.value,
            0L,
            indexType,
        )

        VK10.vkCmdDrawIndexed(commandBuffer.handle, indexCount, 1, 0, 0, 0)
    }
}
