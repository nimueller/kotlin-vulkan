package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.Fence
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkCommandPool
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.vkDestroyCommandPool
import org.lwjgl.vulkan.VK10.vkEndCommandBuffer

/**
 * Represents a Vulkan command pool used for allocating command buffers.
 * Command pools manage the memory used for storing command buffers and provide
 * efficient reuse of command buffer memory allocations.
 *
 * @property logicalDevice The logical device that owns this command pool
 * @property handle The native Vulkan command pool handle
 */
class CommandPool(
    private val logicalDevice: LogicalDevice,
    val handle: VkCommandPool,
) : NativeResource() {

    constructor(logicalDevice: LogicalDevice) : this(logicalDevice, createCommandPool(logicalDevice))

    /**
     * Destroys this command pool and frees associated Vulkan resources.
     * All command buffers allocated from this pool become invalid.
     */
    override fun destroy() {
        vkDestroyCommandPool(logicalDevice.handle, handle.value, null)
    }

    /**
     * Records and executes a one-time command buffer for temporary operations.
     * This method handles the complete lifecycle of a command buffer, including allocation,
     * recording, submission and cleanup.
     *
     * @param queue The Vulkan queue to submit the commands to
     * @param fence Optional fence to signal when the command buffer execution completes.
     *             If null, the method will wait for queue idle instead
     * @param callback Lambda that receives the memory stack and command buffer for recording commands.
     *                All Vulkan commands should be recorded within this callback
     */
    fun recordSingleTimeCommands(
        queue: VkQueue,
        fence: Fence? = null,
        callback: (MemoryStack, VkCommandBuffer) -> Unit,
    ) = MemoryStack.stackPush().use { stack ->
        val commandBuffer = beginSingleTimeCommands(stack)
        callback(stack, commandBuffer)
        endSingleTimeCommands(stack, queue, commandBuffer, fence)
    }

    private fun beginSingleTimeCommands(stack: MemoryStack): VkCommandBuffer {
        val allocateInfo = VkCommandBufferAllocateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            commandBufferCount(1)
            commandPool(handle.value)
        }

        val pCommandBuffers = stack.mallocPointer(1)
        VK10.vkAllocateCommandBuffers(logicalDevice.handle, allocateInfo, pCommandBuffers)
            .validateVulkanSuccess("Allocate command buffer", "Failed to allocate command buffer for buffer copy")

        val beginInfo = VkCommandBufferBeginInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        }

        val commandBuffer = VkCommandBuffer(pCommandBuffers[0], logicalDevice.handle)
        VK10.vkBeginCommandBuffer(commandBuffer, beginInfo)
            .validateVulkanSuccess("Begin command buffer", "Failed to begin command buffer for buffer copy")
        return commandBuffer
    }

    private fun endSingleTimeCommands(
        stack: MemoryStack,
        queue: VkQueue,
        commandBuffer: VkCommandBuffer,
        fence: Fence?,
    ) {
        vkEndCommandBuffer(commandBuffer)
            .validateVulkanSuccess("End command buffer", "Failed to end command buffer for buffer copy")

        val queueSubmitInfo = VkSubmitInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
            pCommandBuffers(stack.pointers(commandBuffer))
        }

        if (fence != null) {
            VK10.vkResetFences(logicalDevice.handle, fence.handle.value)
        }

        VK10.vkQueueSubmit(queue, queueSubmitInfo, fence?.handle?.value ?: VK10.VK_NULL_HANDLE)
            .validateVulkanSuccess("Queue submit", "Failed to submit command buffer for buffer copy")

        if (fence == null) {
            // Wait for Queue idle if no fence is provided
            VK10.vkQueueWaitIdle(queue)
                .validateVulkanSuccess("Queue wait idle", "Failed to wait for command buffer for buffer copy to finish")
        } else {
            VK10.vkWaitForFences(logicalDevice.handle, fence.handle.value, true, Long.MAX_VALUE)
        }

        VK10.vkFreeCommandBuffers(logicalDevice.handle, handle.value, commandBuffer)
    }

    companion object {

        @JvmStatic
        private val logger = logger<CommandPool>()

        /**
         * Creates a new command pool for the specified logical device.
         * The created pool allows individual command buffer resets and is associated with
         * the graphics queue family of the device.
         *
         * @param logicalDevice The logical device to create the command pool for
         * @return A new CommandPool instance
         */
        private fun createCommandPool(logicalDevice: LogicalDevice): VkCommandPool =
            MemoryStack.stackPush().use { stack ->
                val poolCreateInfo = VkCommandPoolCreateInfo.calloc(stack).apply {
                    sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    queueFamilyIndex(logicalDevice.physicalDevice.graphicsQueueFamilyIndex)
                }

                val pCommandPool = stack.mallocLong(1)
                VK10.vkCreateCommandPool(logicalDevice.handle, poolCreateInfo, null, pCommandPool)
                    .validateVulkanSuccess()
                VkCommandPool(pCommandPool[0])
            }.also { commandPool ->
                logger.info("Created command pool: $commandPool")
            }
    }
}
