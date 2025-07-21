package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO
import org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers
import org.lwjgl.vulkan.VK10.vkBeginCommandBuffer
import org.lwjgl.vulkan.VK10.vkEndCommandBuffer
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo

data class CommandBuffer(
    val handle: VkCommandBuffer,
) {

    companion object {

        @JvmStatic
        private val logger = logger<CommandBuffer>()

        fun create(device: LogicalDevice, commandPool: CommandPool) = MemoryStack.stackPush().use { stack ->
            val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                commandPool(commandPool.handle.value)
                level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                commandBufferCount(1)
            }

            val pCommandBuffers = stack.mallocPointer(1)
            vkAllocateCommandBuffers(device.handle, allocateInfo, pCommandBuffers)
                .validateVulkanSuccess()
            CommandBuffer(VkCommandBuffer(pCommandBuffers[0], device.handle))
        }.also { commandBuffer ->
            logger.info("Created command buffer $commandBuffer")
        }
    }

    fun startRecording() = MemoryStack.stackPush().use { stack ->
        val bufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            flags(0)
            pInheritanceInfo(null)
        }

        vkBeginCommandBuffer(handle, bufferBeginInfo)
            .validateVulkanSuccess()
    }

    fun endRecording(commandBuffer: CommandBuffer) {
        vkEndCommandBuffer(commandBuffer.handle)
            .validateVulkanSuccess()
    }
}
