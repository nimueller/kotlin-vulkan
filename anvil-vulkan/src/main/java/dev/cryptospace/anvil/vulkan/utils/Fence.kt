package dev.cryptospace.anvil.vulkan.utils

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkFenceCreateInfo

/**
 * Represents a Vulkan fence synchronization primitive used to synchronize operations between CPU and GPU.
 * Fences can be used to track when submitted commands have completed execution on the GPU.
 */
class Fence(
    /**
     * The logical device that owns this fence.
     */
    private val logicalDevice: LogicalDevice,
    /**
     * The native Vulkan fence handle.
     */
    val handle: VkFence,
) : NativeResource() {

    /**
     * Creates a new fence associated with the specified logical device.
     *
     * @param logicalDevice The logical device to create the fence for
     */
    constructor(logicalDevice: LogicalDevice) : this(logicalDevice, createFence(logicalDevice))

    override fun destroy() {
        VK10.vkDestroyFence(logicalDevice.handle, handle.value, null)
    }

    companion object {

        private fun createFence(logicalDevice: LogicalDevice): VkFence = MemoryStack.stackPush().use { stack ->
            val fenceCreateInfo = VkFenceCreateInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                flags(0)
            }
            val pFence = stack.mallocLong(1)
            VK10.vkCreateFence(logicalDevice.handle, fenceCreateInfo, null, pFence)
            VkFence(pFence[0])
        }
    }
}
