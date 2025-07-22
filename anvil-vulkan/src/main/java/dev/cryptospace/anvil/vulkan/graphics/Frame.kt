package dev.cryptospace.anvil.vulkan.graphics

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
import java.nio.LongBuffer

/**
 * Represents a single frame in the rendering pipeline, managing command buffers and synchronization objects.
 *
 * Frame works in conjunction with SwapChain to coordinate rendering operations and presentation timing.
 * It handles command buffer recording, synchronization between CPU and GPU operations, and frame presentation.
 *
 * The frame manages its own command buffer for recording rendering commands, which is reset and re-recorded
 * for each frame. This ensures efficient resource usage and proper command sequencing.
 *
 * Frame synchronization is handled through multiple semaphores and fences:
 * - Image available semaphores signal when a swapchain image is ready for rendering
 * - Render finished semaphores indicate completion of rendering operations
 * - In-flight fences prevent CPU-GPU race conditions by controlling frame pacing
 *
 * @property logicalDevice The logical device used for executing commands
 * @property renderPass The render pass defining the frame rendering sequence
 * @property graphicsPipeline The pipeline specifying the graphics state for rendering
 * @property swapChain The swap chain used for image presentation
 */
class Frame(
    private val logicalDevice: LogicalDevice,
    private val renderPass: RenderPass,
    private val graphicsPipeline: GraphicsPipeline,
    private val swapChain: SwapChain,
    commandPool: CommandPool,
) : NativeResource() {

    /** Command buffer used for recording and executing rendering commands for this frame */
    private val commandBuffer: CommandBuffer = CommandBuffer.create(logicalDevice, commandPool)

    /**
     * Synchronization primitives managing the timing of rendering and presentation operations.
     * Uses a separate set of semaphores for each swapchain image to avoid synchronization issues.
     */
    private val syncObjects: SyncObjects = SyncObjects(
        logicalDevice = logicalDevice,
        imageCount = swapChain.images.capacity(),
    )

    /**
     * Executes the frame's drawing operations and presents the result to the screen.
     *
     * This method handles:
     * 1. Acquiring the next swap chain image
     * 2. Recording and submitting command buffers
     * 3. Presenting the rendered image to the display
     *
     * The method uses synchronization primitives to ensure proper timing between operations.
     */
    fun draw(): Unit = MemoryStack.stackPush().use { stack ->
        syncObjects.waitForInFlightFence()
        val swapChainImageIndex = acquireSwapChainImage(stack)

        vkResetCommandBuffer(commandBuffer.handle, 0)

        commandBuffer.startRecording()
        val framebuffer = swapChain.framebuffers[swapChainImageIndex]
        renderPass.start(commandBuffer, framebuffer)
        swapChain.recordCommands(commandBuffer, graphicsPipeline)
        renderPass.end(commandBuffer)
        commandBuffer.endRecording()

        presentFrame(stack, swapChain, swapChainImageIndex)
    }

    private fun acquireSwapChainImage(stack: MemoryStack): Int {
        val pImageIndex = stack.mallocInt(1)

        vkAcquireNextImageKHR(
            logicalDevice.handle,
            swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphores.value,
            VK_NULL_HANDLE,
            pImageIndex,
        ).validateVulkanSuccess()

        return pImageIndex[0]
    }

    private fun presentFrame(stack: MemoryStack, swapChain: SwapChain, imageIndex: Int) {
        val signalSemaphores = submitCommandBuffer(stack, imageIndex)
        val swapChains = stack.longs(swapChain.handle.value)

        val presentInto = VkPresentInfoKHR.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            pWaitSemaphores(signalSemaphores)
            swapchainCount(1)
            pSwapchains(swapChains)
            pImageIndices(stack.ints(imageIndex))
            pResults(null)
        }

        vkQueuePresentKHR(logicalDevice.presentQueue, presentInto)
    }

    private fun submitCommandBuffer(stack: MemoryStack, imageIndex: Int): LongBuffer {
        val waitSemaphores = stack.longs(syncObjects.imageAvailableSemaphores.value)
        val waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        val signalSemaphores = stack.longs(syncObjects.renderFinishedSemaphores[imageIndex].value)
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
        return signalSemaphores
    }

    override fun destroy() {
        syncObjects.close()
    }
}
