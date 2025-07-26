package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.native.UniformBufferObject
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.VulkanRenderingContext
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkResetCommandBuffer
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSubmitInfo
import java.nio.LongBuffer
import java.util.EnumSet

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
    private val bufferManager: BufferManager,
) : NativeResource() {

    private val renderPass: RenderPass = logicalDevice.renderPass
    private val graphicsPipeline: GraphicsPipeline = logicalDevice.graphicsPipeline
    private val imageCount: Int = logicalDevice.swapChain.images.capacity()
    private val commandPool: CommandPool = logicalDevice.commandPool
    private val commandBuffer: CommandBuffer = CommandBuffer.create(logicalDevice, commandPool)

    /**
     * Synchronization primitives managing the timing of rendering and presentation operations.
     * Uses a separate set of semaphores for each swapchain image to avoid synchronization issues.
     */
    val syncObjects: SyncObjects = SyncObjects(
        logicalDevice = logicalDevice,
        imageCount = imageCount,
    )

    private val uniformBuffer: BufferAllocation =
        bufferManager.allocateBuffer(
            size = UniformBufferObject.BYTE_SIZE.toLong(),
            usage = EnumSet.of(BufferUsage.UNIFORM_BUFFER),
            properties = EnumSet.of(
                BufferProperties.HOST_VISIBLE,
                BufferProperties.HOST_COHERENT,
            ),
        )

    private val uniformBufferPointer: Long = bufferManager.getPointer(uniformBuffer)

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
    fun draw(swapChainImageIndex: Int, callback: (RenderingContext) -> Unit) {
        vkResetCommandBuffer(commandBuffer.handle, 0)

        commandBuffer.startRecording()
        val framebuffer = logicalDevice.swapChain.framebuffers[swapChainImageIndex]
        renderPass.start(commandBuffer, framebuffer)

        logicalDevice.swapChain.preparePipeline(commandBuffer, graphicsPipeline)

        val renderingContext = VulkanRenderingContext(logicalDevice, commandBuffer)
        callback(renderingContext)

        updateUniformBuffer(renderingContext)

        renderPass.end(commandBuffer)
        commandBuffer.endRecording()

        presentFrame(logicalDevice.swapChain, swapChainImageIndex)
    }

    private fun updateUniformBuffer(renderingContext: VulkanRenderingContext) {
        renderingContext.uniformBufferObject?.let { uniformBufferObject ->
            MemoryStack.stackPush().use { stack ->
                val data = uniformBufferObject.toByteBuffer(stack)
                val dataAddress = MemoryUtil.memAddress(data)
                MemoryUtil.memCopy(dataAddress, uniformBufferPointer, data.remaining().toLong())
            }
        }
    }

    private fun presentFrame(swapChain: SwapChain, imageIndex: Int) = MemoryStack.stackPush().use { stack ->
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
