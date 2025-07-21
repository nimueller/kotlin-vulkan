package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR
import org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR
import org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY
import org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCmdBindPipeline
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VK10.vkCmdSetScissor
import org.lwjgl.vulkan.VK10.vkCmdSetViewport
import org.lwjgl.vulkan.VK10.vkCreateImageView
import org.lwjgl.vulkan.VK10.vkDestroyImageView
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkImageViewCreateInfo
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkViewport
import java.nio.LongBuffer

/**
 * Represents a Vulkan swap chain, which manages a collection of presentable images that can be
 * displayed on the screen. The swap chain is responsible for image presentation and synchronization
 * with the display's refresh rate.
 *
 * The swap chain is closely tied to both the surface and the physical device's presentation capabilities,
 * handling details such as
 * - Image format and color space
 * - Number of images in the chain
 * - Image presentation mode
 * - Image dimensions
 *
 * @param logicalDevice The logical device that owns this swap chain
 * @param handle Native handle to the Vulkan swap chain object
 */
data class SwapChain(
    val logicalDevice: LogicalDevice,
    val handle: Handle,
    val renderPass: RenderPass,
) : NativeResource() {

    /**
     * The dimensions (width and height) of the swap chain images.
     * These dimensions are determined during swap chain creation based on the surface capabilities
     * and window size. All images in the swap chain will have these exact dimensions.
     * This extent is used for various rendering operations and viewport configurations.
     */
    val extent: VkExtent2D = logicalDevice.deviceSurfaceInfo.swapChainDetails.swapChainExtent

    /**
     * Contains the handles to the VkImage objects managed by the swap chain.
     * These images serve as the render targets for the swap chain and are used
     * for presenting content to the screen. The number of images is determined
     * by the swap chain creation parameters and the physical device capabilities.
     *
     * Note: This buffer is allocated using native memory and must be freed
     * when the swap chain is destroyed.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val images: LongBuffer =
        MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBuffer(
                bufferInitializer = { size -> MemoryUtil.memAllocLong(size) },
                bufferQuery = { countBuffer, resultBuffer ->
                    vkGetSwapchainImagesKHR(logicalDevice.handle, handle.value, countBuffer, resultBuffer)
                },
            )
        }

    /**
     * A list of image view handles created from the swap chain images.
     * Image views provide a way to access the image data in a format suitable for rendering,
     * defining how the image should be interpreted (e.g., as a 2D texture).
     * Each image view corresponds to one swap chain image and includes:
     * - Format information matching the swap chain format
     * - Component mapping (RGBA channels)
     * - Subresource range configuration for basic 2D rendering
     *
     * These image views are automatically created during swap chain initialization
     * and are destroyed when the swap chain is destroyed.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val imageViews: List<Handle> =
        MemoryStack.stackPush().use { stack ->
            val resultList = mutableListOf<Handle>()

            images.rewind()
            while (images.hasRemaining()) {
                val image = images.get()

                val createInfo = VkImageViewCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    image(image)
                    viewType(VK_IMAGE_VIEW_TYPE_2D)
                    format(logicalDevice.deviceSurfaceInfo.swapChainDetails.bestSurfaceFormat.format())
                    components { mapping ->
                        mapping.r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.a(VK_COMPONENT_SWIZZLE_IDENTITY)
                    }
                    subresourceRange { range ->
                        range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        range.baseMipLevel(0)
                        range.levelCount(1)
                        range.baseArrayLayer(0)
                        range.layerCount(1)
                    }
                }

                val imageView = stack.mallocLong(1)
                vkCreateImageView(logicalDevice.handle, createInfo, null, imageView).validateVulkanSuccess()
                resultList.add(Handle(imageView[0]))
            }

            resultList
        }

    val framebuffers: List<Framebuffer> =
        Framebuffer.createFramebuffersForSwapChain(logicalDevice, this, renderPass).also { framebuffers ->
            logger.info("Created ${framebuffers.size} framebuffers: $framebuffers")
        }

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    val commandPool =
        CommandPool.create(logicalDevice).also { commandPool ->
            logger.info("Created command pool: $commandPool")
        }

    fun recordCommands(commandBuffer: CommandBuffer, graphicsPipeline: GraphicsPipeline) =
        MemoryStack.stackPush().use { stack ->
            vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.handle.value)

            val viewport = VkViewport.calloc(stack).apply {
                x(0.0f)
                y(0.0f)
                width(extent.width().toFloat())
                height(extent.height().toFloat())
                minDepth(0.0f)
                maxDepth(1.0f)
            }
            val pViewports = VkViewport.calloc(1, stack)
                .put(viewport)
                .flip()
            vkCmdSetViewport(commandBuffer.handle, 0, pViewports)

            val scissor = VkRect2D.calloc(stack).apply {
                offset { it.x(0).y(0) }
                extent { it.width(extent.width()).height(extent.height()) }
            }
            val pScissors = VkRect2D.calloc(1, stack).put(scissor).flip()
            vkCmdSetScissor(commandBuffer.handle, 0, pScissors)

            vkCmdDraw(commandBuffer.handle, 3, 1, 0, 0)
        }

    override fun destroy() {
        commandPool.close()
        framebuffers.forEach { framebuffer ->
            framebuffer.close()
        }
        imageViews.forEach { imageView ->
            vkDestroyImageView(logicalDevice.handle, imageView.value, null)
        }

        MemoryUtil.memFree(images)

        vkDestroySwapchainKHR(logicalDevice.handle, handle.value, null)
    }

    companion object {

        @JvmStatic
        private val logger = logger<SwapChain>()
    }
}
