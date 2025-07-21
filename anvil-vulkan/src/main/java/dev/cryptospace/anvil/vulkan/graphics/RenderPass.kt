package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE
import org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL
import org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass
import org.lwjgl.vulkan.VK10.vkCmdEndRenderPass
import org.lwjgl.vulkan.VK10.vkCreateRenderPass
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass
import org.lwjgl.vulkan.VkAttachmentDescription
import org.lwjgl.vulkan.VkAttachmentReference
import org.lwjgl.vulkan.VkClearColorValue
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkRenderPassCreateInfo
import org.lwjgl.vulkan.VkSubpassDependency
import org.lwjgl.vulkan.VkSubpassDescription

/**
 * Represents a Vulkan render pass that defines the structure and dependencies of rendering operations.
 *
 * @property logicalDevice The logical device that this render pass is associated with.
 */
data class RenderPass(
    val logicalDevice: LogicalDevice,
    val handle: VkRenderPass,
) : NativeResource() {

    companion object {

        @JvmStatic
        private val logger = logger<RenderPass>()

        fun create(logicalDevice: LogicalDevice): RenderPass = MemoryStack.stackPush().use { stack ->
            val colorAttachment = setupColorAttachment(stack, logicalDevice)
            val subpassDescription = setupSubpassDescription(stack)
            val colorAttachmentDescriptions = VkAttachmentDescription.calloc(1, stack)
                .put(colorAttachment)
                .flip()
            val subpassDescriptions = VkSubpassDescription.calloc(1, stack)
                .put(subpassDescription)
                .flip()

            val dependency = VkSubpassDependency.calloc(stack).apply {
                srcSubpass(VK_SUBPASS_EXTERNAL)
                dstSubpass(0)
                srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                srcAccessMask(0)
                dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            }
            val dependencies = VkSubpassDependency.calloc(1, stack)
                .put(dependency)
                .flip()

            val createInfo = VkRenderPassCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                pAttachments(colorAttachmentDescriptions)
                pSubpasses(subpassDescriptions)
                pDependencies(dependencies)
            }

            val renderPassBuffer = stack.mallocLong(1)
            vkCreateRenderPass(logicalDevice.handle, createInfo, null, renderPassBuffer)
                .validateVulkanSuccess()
            RenderPass(logicalDevice, VkRenderPass(renderPassBuffer[0]))
        }.also { renderPass ->
            logger.info("Created render pass: $renderPass")
        }

        private fun setupColorAttachment(stack: MemoryStack, logicalDevice: LogicalDevice): VkAttachmentDescription =
            VkAttachmentDescription.calloc(stack).apply {
                format(logicalDevice.deviceSurfaceInfo.swapChainDetails.bestSurfaceFormat.format())
                samples(VK_SAMPLE_COUNT_1_BIT)
                loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            }

        private fun setupSubpassDescription(stack: MemoryStack): VkSubpassDescription =
            VkSubpassDescription.calloc(stack).apply {
                val colorAttachmentReference = VkAttachmentReference.calloc(stack).apply {
                    attachment(0)
                    layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                }
                val colorAttachmentReferences = VkAttachmentReference.calloc(1, stack)
                    .put(colorAttachmentReference)
                    .flip()

                pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                colorAttachmentCount(colorAttachmentReferences.remaining())
                pColorAttachments(colorAttachmentReferences)
            }
    }

    fun start(commandBuffer: CommandBuffer, framebuffer: Framebuffer) = MemoryStack.stackPush().use { stack ->
        val clearColor = VkClearColorValue.calloc(stack)
            .float32(stack.floats(0.0f))
            .float32(stack.floats(0.0f))
            .float32(stack.floats(0.0f))
            .float32(stack.floats(1.0f))
        val clearValue = VkClearValue.calloc(1, stack)
            .put(VkClearValue.calloc(stack).color(clearColor))
            .flip()

        val beginInfo = VkRenderPassBeginInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
            renderPass(handle.value)
            framebuffer(framebuffer.handle.value)
            renderArea { area ->
                area.offset { offset ->
                    offset.x(0)
                    offset.y(0)
                }
                area.extent { extent ->
                    extent.set(logicalDevice.deviceSurfaceInfo.swapChainDetails.swapChainExtent)
                }
            }
            clearValueCount(clearValue.remaining())
            pClearValues(clearValue)
        }

        vkCmdBeginRenderPass(commandBuffer.handle, beginInfo, VK_SUBPASS_CONTENTS_INLINE)
    }

    fun end(commandBuffer: CommandBuffer) {
        vkCmdEndRenderPass(commandBuffer.handle)
    }

    override fun toString(): String = "RenderPass(handle=$handle)"

    override fun destroy() {
        vkDestroyRenderPass(logicalDevice.handle, handle.value, null)
    }
}
