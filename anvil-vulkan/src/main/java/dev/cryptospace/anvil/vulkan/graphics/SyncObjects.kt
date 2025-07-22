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

/**
 * Manages synchronization primitives for coordinating operations between CPU and GPU.
 *
 * This class creates and manages semaphores and fences used for synchronizing operations:
 * - Image available semaphores: Signal when a swapchain image is available for rendering
 * - Render finished semaphores: Signal when rendering to an image is complete
 * - In-flight fences: Synchronize CPU and GPU operations
 *
 * To avoid synchronization issues, a separate set of semaphores is maintained for each swapchain image.
 *
 * @property logicalDevice The logical device used for creating synchronization objects
 * @property imageCount The number of swapchain images to create semaphores for
 */
class SyncObjects(
    private val logicalDevice: LogicalDevice,
    private val imageCount: Int,
) : NativeResource() {

    val imageAvailableSemaphores: VkSemaphore = createSemaphore(logicalDevice)
    val renderFinishedSemaphores: List<VkSemaphore> = List(imageCount) { createSemaphore(logicalDevice) }
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
    }

    fun resetInFlightFence() {
        vkResetFences(logicalDevice.handle, inFlightFence.value)
    }

    override fun destroy() {
        vkDestroySemaphore(logicalDevice.handle, imageAvailableSemaphores.value, null)

        renderFinishedSemaphores.forEach { semaphore ->
            vkDestroySemaphore(logicalDevice.handle, semaphore.value, null)
        }

        vkDestroyFence(logicalDevice.handle, inFlightFence.value, null)
    }
}
