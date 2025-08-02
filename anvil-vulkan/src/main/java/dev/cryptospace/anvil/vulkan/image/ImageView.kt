package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.handle.VkImageView
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateImageView
import org.lwjgl.vulkan.VK10.vkDestroyImageView
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
     * Creates a new image view with the specified device, image handle, and format.
     *
     * @param device The logical device used to create the image view
     * @param image The Vulkan image handle to create the view for
     * @param format The format of the image view (VkFormat)
     */
    constructor(device: LogicalDevice, image: VkImage, format: Int) : this(
        device,
        createImageView(device, image, format),
    )

    override fun destroy() {
        vkDestroyImageView(device.handle, handle.value, null)
    }

    companion object {

        private fun createImageView(device: LogicalDevice, imageHandle: VkImage, format: Int): VkImageView =
            MemoryStack.stackPush().use { stack ->
                val viewCreateInfo = VkImageViewCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    image(imageHandle.value)
                    viewType(VK_IMAGE_TYPE_2D)
                    format(format)
                    subresourceRange().apply {
                        aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        baseMipLevel(0)
                        levelCount(1)
                        baseArrayLayer(0)
                        layerCount(1)
                    }
                }

                val pImageView = stack.mallocLong(1)
                vkCreateImageView(device.handle, viewCreateInfo, null, pImageView)
                val handle = VkImageView(pImageView[0])
                VkImageView(handle.value)
            }
    }
}
