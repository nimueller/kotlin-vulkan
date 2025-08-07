package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.image.ImageView
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo

data class Framebuffer(
    val device: LogicalDevice,
    val handle: VkImage,
) : NativeResource() {

    override fun destroy() {
        vkDestroyFramebuffer(device.handle, handle.value, null)
    }

    companion object {

        fun createFramebuffersForSwapChain(
            device: LogicalDevice,
            swapChain: SwapChain,
            renderPass: RenderPass,
            depthImageView: ImageView,
        ): List<Framebuffer> = MemoryStack.stackPush().use { stack ->
            val result = ArrayList<Framebuffer>(swapChain.imageViews.size)
            val attachments = stack.mallocLong(2)

            for (imageView in swapChain.imageViews) {
                attachments.clear()
                    .put(imageView.handle.value)
                    .put(depthImageView.handle.value)
                    .flip()

                val framebufferCreateInfo = VkFramebufferCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    renderPass(renderPass.handle.value)
                    pAttachments(attachments)
                    width(swapChain.extent.width())
                    height(swapChain.extent.height())
                    layers(1)
                }

                val pFramebuffer = stack.mallocLong(1)

                vkCreateFramebuffer(
                    swapChain.logicalDevice.handle,
                    framebufferCreateInfo,
                    null,
                    pFramebuffer,
                ).validateVulkanSuccess()

                result.add(Framebuffer(device, VkImage(pFramebuffer[0])))
            }

            result
        }
    }
}
