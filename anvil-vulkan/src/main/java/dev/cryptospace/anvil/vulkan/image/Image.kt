package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateImage
import org.lwjgl.vulkan.VK10.vkDestroyImage
import org.lwjgl.vulkan.VkImageCreateInfo

data class Image(
    private val logicalDevice: LogicalDevice,
    val handle: VkImage,
) : NativeResource() {

    constructor(logicalDevice: LogicalDevice, createInfo: CreateInfo) : this(
        logicalDevice,
        createImage(logicalDevice, createInfo),
    )

    override fun destroy() {
        vkDestroyImage(logicalDevice.handle, handle.value, null)
    }

    data class CreateInfo(
        val width: Int,
        val height: Int,
        val format: Int,
        val usage: Int,
    )

    companion object {

        private fun createImage(logicalDevice: LogicalDevice, createInfo: CreateInfo) =
            MemoryStack.stackPush().use { stack ->
                val imageInfo = VkImageCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    imageType(VK_IMAGE_TYPE_2D)
                    extent().width(createInfo.width)
                    extent().height(createInfo.height)
                    extent().depth(1)
                    mipLevels(1)
                    arrayLayers(1)
                    format(createInfo.format)
                    tiling(VK_IMAGE_TILING_OPTIMAL)
                    initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    usage(createInfo.usage)
                    sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    samples(VK_SAMPLE_COUNT_1_BIT)
                    flags(0)
                }

                val pTextureImage = stack.mallocLong(1)
                vkCreateImage(logicalDevice.handle, imageInfo, null, pTextureImage)
                    .validateVulkanSuccess("Create texture image", "Failed to create texture image")
                VkImage(pTextureImage[0])
            }
    }
}
