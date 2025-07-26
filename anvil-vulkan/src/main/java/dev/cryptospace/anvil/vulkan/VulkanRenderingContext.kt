package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.native.UniformBufferObject
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16
import org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer
import org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers
import org.lwjgl.vulkan.VK10.vkCmdDrawIndexed

class VulkanRenderingContext(
    private val logicalDevice: LogicalDevice,
    private val commandBuffer: CommandBuffer,
) : RenderingContext {

    override val width: Int
        get() = logicalDevice.swapChain.extent.width()

    override val height: Int
        get() = logicalDevice.swapChain.extent.height()

    override var uniformBufferObject: UniformBufferObject = UniformBufferObject.identity

    override fun drawMesh(mesh: Mesh) {
        require(mesh is VulkanMesh) { "Only VulkanMesh is supported" }

        MemoryStack.stackPush().use { stack ->
            val vertexBuffers = stack.longs(mesh.vertexBufferAllocation.buffer.value)
            val offsets = stack.longs(0L)
            vkCmdBindVertexBuffers(commandBuffer.handle, 0, vertexBuffers, offsets)
            vkCmdBindIndexBuffer(
                commandBuffer.handle,
                mesh.indexBufferAllocation.buffer.value,
                0L,
                VK_INDEX_TYPE_UINT16,
            )

            vkCmdDrawIndexed(commandBuffer.handle, mesh.indexCount, 1, 0, 0, 0)
        }
    }
}
