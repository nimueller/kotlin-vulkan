package dev.cryptospace.anvil.vulkan.texture

import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanImage
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED
import org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL
import org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateImage
import org.lwjgl.vulkan.VK10.vkDestroyImage
import org.lwjgl.vulkan.VK10.vkFreeMemory
import org.lwjgl.vulkan.VkImageCreateInfo
import java.nio.ByteBuffer
import java.util.EnumSet

class TextureManager(
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
) : NativeResource() {

    private val textureImages: MutableList<VulkanImage> = mutableListOf()

    fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Image =
        MemoryStack.stackPush().use { stack ->
            // copy pixel data into host visible memory
            bufferManager.allocateBuffer(
                imageSize.toLong(),
                EnumSet.of(BufferUsage.TRANSFER_SRC),
                EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
            ).use { stagingBuffer ->
                bufferManager.uploadData(stagingBuffer, imageData)

                // creating the actual image based on this pixel data
                val imageInfo = VkImageCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    imageType(VK_IMAGE_TYPE_2D)
                    extent().width(width)
                    extent().height(height)
                    extent().depth(1)
                    mipLevels(1)
                    arrayLayers(1)
                    format(VK_FORMAT_R8G8B8A8_SRGB)
                    tiling(VK_IMAGE_TILING_OPTIMAL)
                    initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
                    sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    samples(VK_SAMPLE_COUNT_1_BIT)
                    flags(0)
                }

                val pTextureImage = stack.mallocLong(1)
                vkCreateImage(logicalDevice.handle, imageInfo, null, pTextureImage)
                    .validateVulkanSuccess("Create texture image", "Failed to create texture image")

                // copy image data over to device visible memory...
                val textureImage = VkImage(pTextureImage[0])
                // ... allocating device memory first
                val textureImageMemory = bufferManager.allocateImageBuffer(textureImage)

                // ... converting to optimal layout to transfer to this device memory
                bufferManager.transitionImageLayout(
                    textureImage,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                )
                // ... copying the data from the staging buffer to the device memory
                bufferManager.copyBufferToImage(stagingBuffer.buffer, textureImage, width, height)
                // ... converting to read only optimal layout
                bufferManager.transitionImageLayout(
                    textureImage,
                    VK_FORMAT_R8G8B8A8_SRGB,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                )

                return VulkanImage(textureImage, textureImageMemory).also { image ->
                    textureImages += image
                }
            }
        }

    override fun destroy() {
        for (image in textureImages) {
            vkDestroyImage(logicalDevice.handle, image.textureImage.value, null)
            vkFreeMemory(logicalDevice.handle, image.textureImageMemory.value, null)
        }
    }
}
