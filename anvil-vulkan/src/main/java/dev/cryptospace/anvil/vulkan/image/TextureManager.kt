package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.image.Texture
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanTexture
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkSampler
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSamplerCreateInfo
import java.nio.ByteBuffer

class TextureManager(
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
) : NativeResource() {

    val textureImages: MutableList<VulkanTexture> = mutableListOf()

    val sampler = MemoryStack.stackPush().use { stack ->
        val samplerCreateInfo = VkSamplerCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
            magFilter(VK_FILTER_LINEAR)
            minFilter(VK_FILTER_LINEAR)
            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
            addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
            anisotropyEnable(logicalDevice.physicalDevice.features.samplerAnisotropy())
            maxAnisotropy(determineMaxAnisotropy())
            borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
            unnormalizedCoordinates(false)
            compareEnable(false)
            compareOp(VK_COMPARE_OP_ALWAYS)
            mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
            mipLodBias(0.0f)
            minLod(0.0f)
            maxLod(0.0f)
        }

        val pSampler = stack.mallocLong(1)
        vkCreateSampler(logicalDevice.handle, samplerCreateInfo, null, pSampler)
            .validateVulkanSuccess("Create sampler", "Failed to create sampler")
        VkSampler(pSampler[0])
    }

    private fun determineMaxAnisotropy(): Float {
        if (!logicalDevice.physicalDevice.features.samplerAnisotropy()) {
            return 1.0f
        }

        val properties = logicalDevice.physicalDevice.properties
        return properties.limits().maxSamplerAnisotropy()
    }

    fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Texture =
        MemoryStack.stackPush().use { stack ->
            // copy pixel data into host visible memory
            bufferManager.withStagingBuffer(imageData) { stagingBuffer, fence ->
                val format = VK_FORMAT_R8G8B8A8_SRGB

                // creating the actual image based on this pixel data
                // copy image data over to device visible memory...
                val textureImage = Image(
                    logicalDevice,
                    Image.CreateInfo(
                        width = width,
                        height = height,
                        format = format,
                        usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
                    ),
                )

                // ... allocating device memory first
                val textureImageMemory = bufferManager.allocateImageBuffer(textureImage)

                // ... converting to optimal layout to transfer to this device memory
                bufferManager.transitionImageLayout(
                    textureImage,
                    format,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                )
                // ... copying the data from the staging buffer to the device memory
                bufferManager.copyBufferToImage(fence, stagingBuffer.buffer, textureImage, width, height)
                // ... converting to read only optimal layout
                bufferManager.transitionImageLayout(
                    textureImage,
                    format,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                )

                val textureImageView = ImageView(
                    logicalDevice,
                    textureImage,
                    ImageView.CreateInfo(
                        format = format,
                        aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                    ),
                )

                return@withStagingBuffer VulkanTexture(
                    textureImage,
                    textureImageMemory,
                    textureImageView,
                ).also { image ->
                    textureImages += image
                }
            }
        }

    override fun destroy() {
        vkDestroySampler(logicalDevice.handle, sampler.value, null)

        for (image in textureImages) {
            image.textureImageView.close()
            image.textureImage.close()
            vkFreeMemory(logicalDevice.handle, image.textureImageMemory.value, null)
        }
    }
}
