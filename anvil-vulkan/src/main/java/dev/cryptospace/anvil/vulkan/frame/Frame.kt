package dev.cryptospace.anvil.vulkan.frame

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.VulkanRenderingContext
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.Framebuffer
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.SyncObjects
import dev.cryptospace.anvil.vulkan.pipeline.CameraUniform
import dev.cryptospace.anvil.vulkan.pipeline.InstanceData
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.VkDescriptorSet
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkResetCommandBuffer
import org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSubmitInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet
import java.nio.ByteBuffer
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
 * @property frameDescriptorSet Descriptor set holding this frame's uniform buffer
 * @property renderPass The render pass defining the frame rendering sequence
 */
class Frame(
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
    private val frameDescriptorSet: VkDescriptorSet,
    commandPool: CommandPool,
    private val renderPass: RenderPass,
    private val renderingSystem: VulkanRenderingSystem,
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
            size = CameraUniform.size.toLong(),
            usage = EnumSet.of(BufferUsage.UNIFORM_BUFFER),
            preferredProperties = EnumSet.of(
                BufferProperties.HOST_VISIBLE,
                BufferProperties.HOST_COHERENT,
            ),
            persistentlyMapped = true,
        )
    private val uniformData = checkNotNull(uniformBuffer.mappedData)

    private var instanceBuffer: BufferAllocation = allocateInstanceBuffer(INITIAL_INSTANCE_CAPACITY)

    /** Number of instances the instance buffer can hold. */
    var instanceCapacity: Int = INITIAL_INSTANCE_CAPACITY
        private set

    init {
        writeBufferDescriptor(BINDING_CAMERA, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, uniformBuffer)
        writeBufferDescriptor(BINDING_INSTANCES, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, instanceBuffer)
    }

    private fun allocateInstanceBuffer(capacity: Int): BufferAllocation = bufferManager.allocateBuffer(
        size = capacity.toLong() * InstanceData.size,
        usage = EnumSet.of(BufferUsage.STORAGE_BUFFER),
        preferredProperties = EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
        persistentlyMapped = true,
    )

    /**
     * Returns this frame's mapped instance memory, with room for at least [count] [InstanceData] elements.
     *
     * Must be called while recording this frame, before its descriptor set is bound: growing the buffer rewrites the
     * descriptor. That is safe at this point because the frame's fence was waited on, so the GPU no longer uses either
     * the old buffer or the descriptor set.
     */
    fun reserveInstances(count: Int): ByteBuffer {
        if (count > instanceCapacity) {
            var capacity = instanceCapacity
            while (capacity < count) {
                capacity = Math.multiplyExact(capacity, 2)
            }

            bufferManager.freeBuffer(instanceBuffer)
            instanceBuffer = allocateInstanceBuffer(capacity)
            instanceCapacity = capacity
            writeBufferDescriptor(BINDING_INSTANCES, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, instanceBuffer)
        }

        return checkNotNull(instanceBuffer.mappedData)
    }

    private fun writeBufferDescriptor(binding: Int, descriptorType: Int, buffer: BufferAllocation) =
        MemoryStack.stackPush().use { stack ->
            val bufferInfos = VkDescriptorBufferInfo.calloc(1, stack)
            bufferInfos[0]
                .buffer(buffer.buffer.value)
                .offset(0)
                .range(buffer.size)

            val writes = VkWriteDescriptorSet.calloc(1, stack)
            writes[0]
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(frameDescriptorSet.value)
                .dstBinding(binding)
                .dstArrayElement(0)
                .descriptorType(descriptorType)
                .descriptorCount(1)
                .pBufferInfo(bufferInfos)

            vkUpdateDescriptorSets(logicalDevice.handle, writes, null)
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
    fun draw(engine: Engine, callback: (CommandBuffer, VkDescriptorSet, RenderingContext) -> Unit): FrameDrawResult =
        MemoryStack.stackPush().use { stack ->
            val swapChainImageIndex = acquireSwapChainImage(stack)
                ?: return FrameDrawResult.SKIPPED_SWAP_CHAIN_OUTDATED

            val framebuffer = renderingSystem.swapChain.framebuffers[swapChainImageIndex]

            prepareRecordCommands(framebuffer)
            recordCommands(stack, engine, callback)
            finishRecordCommands()

            return when (val result = presentFrame(renderingSystem.swapChain, swapChainImageIndex)) {
                VK_ERROR_OUT_OF_DATE_KHR, VK_SUBOPTIMAL_KHR -> FrameDrawResult.PRESENTED_SWAP_CHAIN_OUTDATED

                else -> {
                    result.validateVulkanSuccess("Present frame", "Failed to present frame")
                    FrameDrawResult.SUCCESS
                }
            }
        }

    /**
     * Waits until this frame's previous submission finished and acquires the next swap chain image.
     *
     * @return The image index, or null if the swap chain is out of date and must be recreated first
     */
    private fun acquireSwapChainImage(stack: MemoryStack): Int? {
        syncObjects.waitForInFlightFence()

        val pImageIndex = stack.mallocInt(1)
        val result = vkAcquireNextImageKHR(
            logicalDevice.handle,
            renderingSystem.swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphores.value,
            VK_NULL_HANDLE,
            pImageIndex,
        )

        // Only reset the fence once work is guaranteed to be submitted, otherwise the next wait would never return.
        // A suboptimal swap chain still delivered an image, so render it and let presentation trigger the recreation.
        when (result) {
            VK_ERROR_OUT_OF_DATE_KHR -> return null
            VK_SUBOPTIMAL_KHR -> Unit
            else -> result.validateVulkanSuccess("Acquire swap chain image", "Acquire swap chain image failed")
        }

        syncObjects.resetInFlightFence()
        return pImageIndex[0]
    }

    private fun prepareRecordCommands(framebuffer: Framebuffer) {
        vkResetCommandBuffer(commandBuffer.handle, 0)

        commandBuffer.startRecording()
        renderPass.start(commandBuffer, framebuffer)
        renderingSystem.swapChain.preparePipeline(commandBuffer)
    }

    private fun recordCommands(
        stack: MemoryStack,
        engine: Engine,
        callback: (CommandBuffer, VkDescriptorSet, RenderingContext) -> Unit,
    ) {
        val renderingContext = VulkanRenderingContext(engine, renderingSystem.swapChain)
        callback(commandBuffer, frameDescriptorSet, renderingContext)

        // Safe to overwrite: this frame's fence was waited on, so the GPU no longer reads the previous contents
        val camera = renderingContext.engine.camera
        CameraUniform.view.put(uniformData, camera.view)
        CameraUniform.projection.put(uniformData, camera.projection)
        bufferManager.flush(uniformBuffer)
        bufferManager.flush(instanceBuffer)
    }

    private fun finishRecordCommands() {
        renderPass.end(commandBuffer)
        commandBuffer.endRecording()
    }

    /**
     * Submits the recorded commands and queues the image for presentation.
     *
     * @return The result of `vkQueuePresentKHR`
     */
    private fun presentFrame(swapChain: SwapChain, imageIndex: Int): Int = MemoryStack.stackPush().use { stack ->
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

    private companion object {
        const val BINDING_CAMERA = 0
        const val BINDING_INSTANCES = 1
        const val INITIAL_INSTANCE_CAPACITY = 1024
    }
}
