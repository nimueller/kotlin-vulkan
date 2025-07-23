package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import org.lwjgl.vulkan.VK10.vkDestroyBuffer
import org.lwjgl.vulkan.VK10.vkFreeMemory

/**
 * Represents a Vulkan buffer resource with its associated memory.
 *
 * This class encapsulates a Vulkan buffer and its bound memory, providing
 * proper resource management and cleanup. It implements NativeResource to
 * ensure proper destruction of native Vulkan resources.
 *
 * @property logicalDevice The logical device that created this buffer
 * @property buffer The Vulkan buffer handle
 * @property memory The device memory allocated for the buffer
 */
class BufferResource(
    private val logicalDevice: LogicalDevice,
    val buffer: VkBuffer,
    val memory: VkDeviceMemory
) : NativeResource() {

    /**
     * Destroys the buffer and frees its associated memory.
     * Resources are destroyed in the correct order: buffer first, then memory.
     */
    override fun destroy() {
        // Destroy the buffer
        vkDestroyBuffer(logicalDevice.handle, buffer.value, null)

        // Free the memory
        vkFreeMemory(logicalDevice.handle, memory.value, null)
    }

    override fun toString(): String = "BufferResource(buffer=$buffer, memory=$memory)"
}
