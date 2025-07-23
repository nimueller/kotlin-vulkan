package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkCommandPool
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateCommandPool
import org.lwjgl.vulkan.VK10.vkDestroyCommandPool
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

/**
 * Represents a Vulkan command pool used for allocating command buffers.
 * Command pools manage the memory used for storing command buffers and provide
 * efficient reuse of command buffer memory allocations.
 *
 * @property logicalDevice The logical device that owns this command pool
 * @property handle The native Vulkan command pool handle
 */
data class CommandPool(
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
                    sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    queueFamilyIndex(logicalDevice.physicalDevice.graphicsQueueFamilyIndex)
                }

                val pCommandPool = stack.mallocLong(1)
                vkCreateCommandPool(logicalDevice.handle, poolCreateInfo, null, pCommandPool)
                    .validateVulkanSuccess()
                VkCommandPool(pCommandPool[0])
            }.also { commandPool ->
                logger.info("Created command pool: $commandPool")
            }
    }
}
