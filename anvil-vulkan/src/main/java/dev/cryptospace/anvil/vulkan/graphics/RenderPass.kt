package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkRenderPass
import dev.cryptospace.anvil.vulkan.toBuffer
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*

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

    fun start(commandBuffer: CommandBuffer, framebuffer: Framebuffer) = MemoryStack.stackPush().use { stack ->
        val clearColor = VkClearColorValue.calloc(stack)
            .float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f))
        val clearValues = VkClearValue.calloc(1, stack)
            .put(VkClearValue.calloc(stack).color(clearColor))
            .put(
                VkClearValue.calloc(stack).depthStencil { stencil ->
                    stencil.depth(1.0f)
                },
            )
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
                    extent.set(logicalDevice.physicalDeviceSurfaceInfo.swapChainDetails.swapChainExtent)
                }
            }
            clearValueCount(clearValues.remaining())
            pClearValues(clearValues)
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
                stencilLoadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                stencilStoreOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
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
