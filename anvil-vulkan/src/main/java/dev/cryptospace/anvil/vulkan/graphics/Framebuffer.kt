package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateFramebuffer
import org.lwjgl.vulkan.VK10.vkDestroyFramebuffer
import org.lwjgl.vulkan.VkFramebufferCreateInfo

data class Framebuffer(
    val device: LogicalDevice,
    val handle: Handle,
) : NativeResource() {

    override fun destroy() {
        vkDestroyFramebuffer(device.handle, handle.value, null)
    }

    companion object {

        fun createFramebuffersForSwapChain(
            device: LogicalDevice,
            swapChain: SwapChain,
            renderPass: RenderPass,
        ): List<Framebuffer> = MemoryStack.stackPush().use { stack ->
            val result = ArrayList<Framebuffer>(swapChain.imageViews.size)
            val attachments = stack.mallocLong(1)

            for (imageView in swapChain.imageViews) {
                attachments.clear()
                    .put(imageView.value)
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

                result.add(Framebuffer(device, Handle(pFramebuffer[0])))
            }

            result
        }
    }
}
