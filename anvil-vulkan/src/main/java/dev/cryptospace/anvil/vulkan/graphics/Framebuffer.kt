package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateFramebuffer
import org.lwjgl.vulkan.VkFramebufferCreateInfo

data class Framebuffer(
    val swapChain: SwapChain,
    val renderPass: RenderPass,
) : NativeResource() {

    val framebuffers =
        MemoryStack.stackPush().use { stack ->
            val swapChainFramebuffers = stack.mallocLong(swapChain.imageViews.size)

            swapChain.imageViews.map { imageViewHandle ->
                val attachments = stack.longs(imageViewHandle.value)

                val framebufferInfo = VkFramebufferCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    renderPass(renderPass.handle.value)
                    pAttachments(attachments)
                    width(swapChain.extent.width())
                    height(swapChain.extent.height())
                    layers(1)
                }

                vkCreateFramebuffer(
                    swapChain.logicalDevice.handle,
                    framebufferInfo,
                    null,
                    swapChainFramebuffers,
                ).validateVulkanSuccess()
            }
        }

    override fun destroy() {
        // Nothing to do
    }
}
