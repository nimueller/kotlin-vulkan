package dev.cryptospace.anvil.vulkan.frame

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.native.UniformBufferObject
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.VulkanRenderingContext
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.*
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSet
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.nio.LongBuffer
import java.util.*

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
    private val textureManager: TextureManager,
    private val frameDescriptorSet: VkDescriptorSet,
    private val materialDescriptorSet: VkDescriptorSet,
    private val commandPool: CommandPool,
    private val renderPass: RenderPass,
    private val renderingSystem: VulkanRenderingSystem,
    private val graphicsPipelineTextured3D: GraphicsPipeline,
) : NativeResource() {

    private val imageCount: Int = renderingSystem.swapChain.images.size
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
            preferredProperties = EnumSet.of(
                BufferProperties.HOST_VISIBLE,
                BufferProperties.HOST_COHERENT,
            ),
        )

    // TODO
//    private val uniformBufferPointer: Long = bufferManager.getPointer(uniformBuffer)

    init {
        updateDescriptorSets()
    }

    fun updateDescriptorSets() {
        MemoryStack.stackPush().use { stack ->
            val descriptorBufferInfo = VkDescriptorBufferInfo.calloc(stack)
                .offset(0)
                .range(UniformBufferObject.BYTE_SIZE.toLong())
                .buffer(uniformBuffer.buffer.value)

            val descriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, stack)
                .put(descriptorBufferInfo)
                .flip()

            val uniformBufferDescriptorWrite = VkWriteDescriptorSet.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(frameDescriptorSet.value)
                .dstBinding(0)
                .dstArrayElement(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .pBufferInfo(descriptorBufferInfos)
                .pImageInfo(null)
                .pTexelBufferView(null)

            // TODO this is temporary and texture should be dynamic per model
            val texture = textureManager.textureImages.firstOrNull()

            val writeDescriptorSets = if (texture == null) {
                VkWriteDescriptorSet.calloc(1, stack)
                    .put(uniformBufferDescriptorWrite)
                    .flip()
            } else {
                val imageInfo = VkDescriptorImageInfo.calloc(stack)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.textureImageView.handle.value)
                    .sampler(textureManager.sampler.value)
                val imageInfos = VkDescriptorImageInfo.calloc(1, stack)
                    .put(imageInfo)
                    .flip()
                val imageInfoDescriptorWrite = VkWriteDescriptorSet.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(materialDescriptorSet.value)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfos)
                VkWriteDescriptorSet.calloc(2, stack)
                    .put(uniformBufferDescriptorWrite)
                    .put(imageInfoDescriptorWrite)
                    .flip()
            }

            vkUpdateDescriptorSets(logicalDevice.handle, writeDescriptorSets, null)
        }
    }

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
    fun draw(engine: Engine, callback: (CommandBuffer, RenderingContext) -> Unit): FrameDrawResult =
        MemoryStack.stackPush().use { stack ->
            val swapChainImageIndex = acquireSwapChainImage(stack) ?: return FrameDrawResult.FRAMEBUFFER_RESIZED

            val framebuffer = renderingSystem.swapChain.framebuffers[swapChainImageIndex]

            prepareRecordCommands(stack, framebuffer)
            recordCommands(stack, engine, callback)
            finishRecordCommands()

            presentFrame(renderingSystem.swapChain, swapChainImageIndex)
            return FrameDrawResult.SUCCESS
        }

    private fun acquireSwapChainImage(stack: MemoryStack): Int? {
        syncObjects.waitForInFlightFence()

        try {
            val pImageIndex = stack.mallocInt(1)

            val result = vkAcquireNextImageKHR(
                logicalDevice.handle,
                renderingSystem.swapChain.handle.value,
                Long.MAX_VALUE,
                syncObjects.imageAvailableSemaphores.value,
                VK_NULL_HANDLE,
                pImageIndex,
            )

            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                return null
            } else {
                result.validateVulkanSuccess("Acquire swap chain image", "Acquire swap chain image failed")
            }

            return pImageIndex[0]
        } finally {
            syncObjects.resetInFlightFence()
        }
    }

    private fun prepareRecordCommands(stack: MemoryStack, framebuffer: Framebuffer) {
        vkResetCommandBuffer(commandBuffer.handle, 0)

        commandBuffer.startRecording()
        renderPass.start(commandBuffer, framebuffer)

        val pipeline = graphicsPipelineTextured3D
        renderingSystem.swapChain.preparePipeline(commandBuffer, pipeline)
        vkCmdBindDescriptorSets(
            commandBuffer.handle,
            VK_PIPELINE_BIND_POINT_GRAPHICS,
            pipeline.pipelineLayoutHandle.value,
            0,
            stack.longs(frameDescriptorSet.value, materialDescriptorSet.value),
            null,
        )
    }

    private fun recordCommands(
        stack: MemoryStack,
        engine: Engine,
        callback: (CommandBuffer, RenderingContext) -> Unit,
    ) {
        val renderingContext = VulkanRenderingContext(engine, renderingSystem.swapChain)
        callback(commandBuffer, renderingContext)

        val camera = renderingContext.engine.camera
        bufferManager.uploadData(uniformBuffer, camera.toByteBuffer(stack))
    }

    private fun finishRecordCommands() {
        renderPass.end(commandBuffer)
        commandBuffer.endRecording()
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
