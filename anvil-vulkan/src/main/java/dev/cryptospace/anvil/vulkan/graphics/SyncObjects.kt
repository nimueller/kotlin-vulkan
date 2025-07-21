package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkFence
import dev.cryptospace.anvil.vulkan.handle.VkSemaphore
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateFence
import org.lwjgl.vulkan.VK10.vkCreateSemaphore
import org.lwjgl.vulkan.VK10.vkDestroyFence
import org.lwjgl.vulkan.VK10.vkDestroySemaphore
import org.lwjgl.vulkan.VK10.vkResetFences
import org.lwjgl.vulkan.VK10.vkWaitForFences
import org.lwjgl.vulkan.VkFenceCreateInfo
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

class SyncObjects(
    private val logicalDevice: LogicalDevice,
) : NativeResource() {

    val imageAvailableSemaphore: VkSemaphore = createSemaphore(logicalDevice)
    val renderFinishedSemaphore: VkSemaphore = createSemaphore(logicalDevice)
    val inFlightFence: VkFence = createFence(logicalDevice)

    private fun createSemaphore(logicalDevice: LogicalDevice): VkSemaphore = MemoryStack.stackPush().use { stack ->
        val createInfo = VkSemaphoreCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
        }
        val pSemaphore = stack.mallocLong(1)
        vkCreateSemaphore(logicalDevice.handle, createInfo, null, pSemaphore)
            .validateVulkanSuccess()
        VkSemaphore(pSemaphore[0])
    }

    private fun createFence(logicalDevice: LogicalDevice): VkFence = MemoryStack.stackPush().use { stack ->
        val createInfo = VkFenceCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
            flags(VK_FENCE_CREATE_SIGNALED_BIT)
        }
        val pFence = stack.mallocLong(1)
        vkCreateFence(logicalDevice.handle, createInfo, null, pFence)
            .validateVulkanSuccess()
        VkFence(pFence[0])
    }

    fun waitForInFlightFence() {
        vkWaitForFences(logicalDevice.handle, inFlightFence.value, true, Long.MAX_VALUE)
        vkResetFences(logicalDevice.handle, inFlightFence.value)
    }

    override fun destroy() {
        vkDestroySemaphore(logicalDevice.handle, imageAvailableSemaphore.value, null)
        vkDestroySemaphore(logicalDevice.handle, renderFinishedSemaphore.value, null)
        vkDestroyFence(logicalDevice.handle, inFlightFence.value, null)
    }
}
