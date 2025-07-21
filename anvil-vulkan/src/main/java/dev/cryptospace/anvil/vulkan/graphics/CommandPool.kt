package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateCommandPool
import org.lwjgl.vulkan.VK10.vkDestroyCommandPool
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

data class CommandPool(
    val logicalDevice: LogicalDevice,
    val handle: Handle,
) : NativeResource() {

    companion object {

        fun create(logicalDevice: LogicalDevice): CommandPool = MemoryStack.stackPush().use { stack ->
            val poolCreateInfo = VkCommandPoolCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                queueFamilyIndex(logicalDevice.physicalDevice.graphicsQueueFamilyIndex)
            }

            val pCommandPool = stack.mallocLong(1)
            vkCreateCommandPool(logicalDevice.handle, poolCreateInfo, null, pCommandPool)
                .validateVulkanSuccess()
            CommandPool(logicalDevice, Handle(pCommandPool[0]))
        }
    }

    override fun destroy() {
        vkDestroyCommandPool(logicalDevice.handle, handle.value, null)
    }
}
