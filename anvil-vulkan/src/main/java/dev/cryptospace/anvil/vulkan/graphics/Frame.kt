package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkResetCommandBuffer
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSubmitInfo

class Frame(
    private val logicalDevice: LogicalDevice,
    private val swapChain: SwapChain,
) : NativeResource() {

    companion object {

        @JvmStatic
        private val logger = logger<Frame>()
    }

    val commandBuffer: CommandBuffer = CommandBuffer.create(logicalDevice, swapChain.commandPool)

    val syncObjects: SyncObjects = SyncObjects(logicalDevice)

    fun draw(): Unit = MemoryStack.stackPush().use { stack ->
        syncObjects.waitForInFlightFence()
        val pImageIndex = stack.mallocInt(1)

        vkAcquireNextImageKHR(
            logicalDevice.handle,
            swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphore.value,
            VK_NULL_HANDLE,
            pImageIndex,
        ).validateVulkanSuccess()

        vkResetCommandBuffer(commandBuffer.handle, 0)
        swapChain.recordCommands(commandBuffer, pImageIndex[0])

        val waitSemaphores = stack.longs(syncObjects.imageAvailableSemaphore.value)
        val waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        val signalSemaphores = stack.longs(syncObjects.renderFinishedSemaphore.value)
        val commandBuffers = stack.pointers(commandBuffer.handle)

        val submitInfo = VkSubmitInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            waitSemaphoreCount(1)
            pWaitSemaphores(waitSemaphores)
            pWaitDstStageMask(waitStages)
            pCommandBuffers(commandBuffers)
            pSignalSemaphores(signalSemaphores)
        }

        vkQueueSubmit(logicalDevice.graphicsQueue, submitInfo, syncObjects.inFlightFence.value)
            .validateVulkanSuccess()

        val swapChains = stack.longs(swapChain.handle.value)
        val presentInto = VkPresentInfoKHR.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            pWaitSemaphores(signalSemaphores)
            swapchainCount(1)
            pSwapchains(swapChains)
            pImageIndices(pImageIndex)
            pResults(null)
        }

        vkQueuePresentKHR(logicalDevice.presentQueue, presentInto)
    }

    override fun destroy() {
        syncObjects.close()
    }
}
