package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE
import org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateRenderPass
import org.lwjgl.vulkan.VK10.vkDestroyRenderPass
import org.lwjgl.vulkan.VkAttachmentDescription
import org.lwjgl.vulkan.VkAttachmentReference
import org.lwjgl.vulkan.VkRenderPassCreateInfo
import org.lwjgl.vulkan.VkSubpassDescription

/**
 * Represents a Vulkan render pass that defines the structure and dependencies of rendering operations.
 *
 * @property logicalDevice The logical device that this render pass is associated with.
 */
class RenderPass(
    private val logicalDevice: LogicalDevice,
) : NativeResource() {

    private fun setupColorAttachment(stack: MemoryStack): VkAttachmentDescription =
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
                layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            }

            pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            pColorAttachments(VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentReference))
        }

    /**
     * The native handle to the Vulkan render pass object.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val handle: Handle =
        MemoryStack.stackPush().use { stack ->
            val colorAttachment = setupColorAttachment(stack)
            val subpassDescription = setupSubpassDescription(stack)

            val createInfo = VkRenderPassCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                pAttachments(VkAttachmentDescription.calloc(1, stack).put(0, colorAttachment))
                pSubpasses(VkSubpassDescription.calloc(1, stack).put(0, subpassDescription))
            }

            val renderPassBuffer = stack.mallocLong(1)
            vkCreateRenderPass(logicalDevice.handle, createInfo, null, renderPassBuffer).validateVulkanSuccess()
            Handle(renderPassBuffer[0])
        }

    override fun destroy() {
        vkDestroyRenderPass(logicalDevice.handle, handle.value, null)
    }
}
