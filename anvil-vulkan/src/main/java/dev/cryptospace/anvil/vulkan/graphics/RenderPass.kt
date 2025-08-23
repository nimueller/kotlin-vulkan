package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkRenderPass
import dev.cryptospace.anvil.vulkan.toBuffer
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
import org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT
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

    constructor(logicalDevice: LogicalDevice) : this(logicalDevice, createRenderPass(logicalDevice))

    /**
     * Begins a render pass operation on the specified command buffer using the provided framebuffer.
     *
     * This method initializes the render pass with clear values for both color and depth/stencil attachments.
     * The color attachment is cleared to black with full opacity (0,0,0,1), and the depth attachment is cleared
     * to 1.0f. The render area is set to the full extent of the swap chain.
     *
     * @param commandBuffer The command buffer to record the render pass commands into
     * @param framebuffer The framebuffer to be used for this render pass instance
     */
    fun start(commandBuffer: CommandBuffer, framebuffer: Framebuffer) = MemoryStack.stackPush().use { stack ->
        val clearValues = listOf(
            VkClearValue.calloc(stack).color(
                VkClearColorValue.calloc(stack)
                    .float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f)),
            ),
            VkClearValue.calloc(stack).depthStencil { stencil ->
                stencil.depth(1.0f)
            },
        ).toBuffer { size ->
            VkClearValue.calloc(size, stack)
        }

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
                    extent.set(logicalDevice.physicalDeviceSurfaceInfo.swapChainDetails.swapChainExtent)
                }
            }
            clearValueCount(clearValues.remaining())
            pClearValues(clearValues)
        }

        vkCmdBeginRenderPass(commandBuffer.handle, beginInfo, VK_SUBPASS_CONTENTS_INLINE)
    }

    /**
     * Ends the current render pass operation on the specified command buffer.
     *
     * This method must be called after all rendering commands within the render pass
     * have been recorded. It signals the end of the render pass instance.
     *
     * @param commandBuffer The command buffer in which to record the end render pass command
     */
    fun end(commandBuffer: CommandBuffer) {
        vkCmdEndRenderPass(commandBuffer.handle)
    }

    override fun toString(): String = "RenderPass(handle=$handle)"

    override fun destroy() {
        vkDestroyRenderPass(logicalDevice.handle, handle.value, null)
    }

    companion object {

        @JvmStatic
        private val logger = logger<RenderPass>()

        private fun createRenderPass(logicalDevice: LogicalDevice): VkRenderPass =
            MemoryStack.stackPush().use { stack ->
                val depthAttachment = setupDepthAttachment(stack)
                val attachments = listOf(
                    setupColorAttachment(stack, logicalDevice),
                    depthAttachment,
                )
                val depthAttachmentReference = VkAttachmentReference.calloc(stack).apply {
                    layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                    attachment(attachments.indexOf(depthAttachment))
                }

                val createInfo = VkRenderPassCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    pAttachments(
                        attachments.toBuffer { size ->
                            VkAttachmentDescription.calloc(size, stack)
                        },
                    )
                    pSubpasses(
                        listOf(
                            setupSubpassDescription(stack, depthAttachmentReference),
                        ).toBuffer { size ->
                            VkSubpassDescription.calloc(size, stack)
                        },
                    )
                    pDependencies(
                        listOf(
                            setupColorDependency(stack),
                            setupDepthDependency(stack),
                        ).toBuffer { size ->
                            VkSubpassDependency.calloc(size, stack)
                        },
                    )
                }

                val renderPassBuffer = stack.mallocLong(1)
                vkCreateRenderPass(logicalDevice.handle, createInfo, null, renderPassBuffer)
                    .validateVulkanSuccess()
                VkRenderPass(renderPassBuffer[0])
            }.also { renderPass ->
                logger.debug { "Created render pass: $renderPass" }
            }

        private fun setupColorAttachment(stack: MemoryStack, logicalDevice: LogicalDevice): VkAttachmentDescription =
            VkAttachmentDescription.calloc(stack).apply {
                format(logicalDevice.physicalDeviceSurfaceInfo.swapChainDetails.bestSurfaceFormat.format())
                samples(VK_SAMPLE_COUNT_1_BIT)
                loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                flags(0)
            }

        private fun setupDepthAttachment(stack: MemoryStack): VkAttachmentDescription =
            VkAttachmentDescription.calloc(stack).apply {
                format(VK_FORMAT_D32_SFLOAT)
                samples(VK_SAMPLE_COUNT_1_BIT)
                loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                flags(0)
            }

        private fun setupSubpassDescription(
            stack: MemoryStack,
            depthAttachmentReference: VkAttachmentReference,
        ): VkSubpassDescription = VkSubpassDescription.calloc(stack).apply {
            val colorAttachmentReferences = listOf(
                VkAttachmentReference.calloc(stack).apply {
                    attachment(0)
                    layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                },
            ).toBuffer { size ->
                VkAttachmentReference.calloc(size, stack)
            }

            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            colorAttachmentCount(colorAttachmentReferences.remaining())
            pColorAttachments(colorAttachmentReferences)
            pDepthStencilAttachment(depthAttachmentReference)
        }

        private fun setupColorDependency(stack: MemoryStack): VkSubpassDependency =
            VkSubpassDependency.calloc(stack).apply {
                srcSubpass(VK_SUBPASS_EXTERNAL)
                dstSubpass(0)
                srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                srcAccessMask(0)
                dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            }

        private fun setupDepthDependency(stack: MemoryStack): VkSubpassDependency =
            VkSubpassDependency.calloc(stack).apply {
                srcSubpass(VK_SUBPASS_EXTERNAL)
                dstSubpass(0)
                srcStageMask(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                srcAccessMask(0)
                dstStageMask(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
            }
    }
}
