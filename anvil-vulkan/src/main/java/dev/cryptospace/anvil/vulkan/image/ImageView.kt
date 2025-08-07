package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkImageView
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageViewCreateInfo

/**
 * A convenient wrapper around the Vulkan image view that simplifies its creation and management.
 *
 * This class encapsulates the native Vulkan image view handle and provides automatic resource cleanup.
 *
 * @property device The logical device that created this image view
 * @property handle The Vulkan handle for this image view
 */
data class ImageView(
    private val device: LogicalDevice,
    val handle: VkImageView,
) : NativeResource() {

    /**
     * Creates a new image view with the specified configuration.
     *
     * @param device The logical device used to create the image view
     * @param image The source image to create the view for
     * @param createInfo Configuration parameters for the image view creation
     */
    constructor(device: LogicalDevice, image: Image, createInfo: CreateInfo) : this(
        device,
        createImageView(device, image, createInfo),
    )

    override fun destroy() {
        vkDestroyImageView(device.handle, handle.value, null)
    }

    data class CreateInfo(
        val format: Int,
        val aspectMask: Int,
    )

    companion object {

        private fun createImageView(device: LogicalDevice, imageHandle: Image, createInfo: CreateInfo): VkImageView =
            MemoryStack.stackPush().use { stack ->
                val viewCreateInfo = VkImageViewCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    image(imageHandle.handle.value)
                    viewType(VK_IMAGE_TYPE_2D)
                    format(createInfo.format)
                    components { mapping ->
                        mapping.r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        mapping.a(VK_COMPONENT_SWIZZLE_IDENTITY)
                    }
                    subresourceRange().apply {
                        aspectMask(createInfo.aspectMask)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }
                }

                val pImageView = stack.mallocLong(1)
                vkCreateImageView(device.handle, viewCreateInfo, null, pImageView)
                    .validateVulkanSuccess("Create image view", "Failed to create image view")
                val handle = VkImageView(pImageView[0])
                VkImageView(handle.value)
            }
    }
}
