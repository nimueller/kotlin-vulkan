package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*

data class VulkanMesh(
    override var modelMatrix: Mat4 = Mat4.identity,
    val vertexBufferAllocation: BufferAllocation,
    val indexBufferAllocation: BufferAllocation,
    val indexCount: Int,
    val indexType: Int,
    val graphicsPipeline: GraphicsPipeline,
) : Mesh {

    fun draw(stack: MemoryStack, logicalDevice: LogicalDevice, commandBuffer: CommandBuffer) {
        val vertexBuffers = stack.longs(vertexBufferAllocation.buffer.value)
        val offsets = stack.longs(0L)

        vkCmdPushConstants(
            commandBuffer.handle,
            logicalDevice.graphicsPipelineTextured2D.pipelineLayoutHandle.value,
            VK_SHADER_STAGE_VERTEX_BIT,
            0,
            modelMatrix.toByteBuffer(stack),
        )
        vkCmdBindVertexBuffers(commandBuffer.handle, 0, vertexBuffers, offsets)
        vkCmdBindIndexBuffer(
            commandBuffer.handle,
            indexBufferAllocation.buffer.value,
            0L,
            indexType,
        )

        vkCmdDrawIndexed(commandBuffer.handle, indexCount, 1, 0, 0, 0)
    }
}
