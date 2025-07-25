package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import org.lwjgl.vulkan.VK10.vkDestroyBuffer
import org.lwjgl.vulkan.VK10.vkFreeMemory

data class BufferAllocation(
    private val logicalDevice: LogicalDevice,
    val buffer: VkBuffer,
    val memory: VkDeviceMemory,
    val size: Long,
) : NativeResource() {

    override fun destroy() {
        vkDestroyBuffer(logicalDevice.handle, buffer.value, null)

        vkFreeMemory(logicalDevice.handle, memory.value, null)
    }
}
