package dev.cryptospace.anvil.vulkan.image

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.Fence
import dev.cryptospace.anvil.vulkan.VulkanTexture
import dev.cryptospace.anvil.vulkan.buffer.Allocator
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.VkBuffer
import dev.cryptospace.anvil.vulkan.buffer.VmaAllocation
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.handle.VkSampler
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkSamplerCreateInfo
import java.nio.ByteBuffer

class TextureManager(
    private val allocator: Allocator,
    private val logicalDevice: LogicalDevice,
    private val bufferManager: BufferManager,
    private val commandPool: CommandPool,
) : NativeResource() {

    val textureImages: MutableList<VulkanTexture> = mutableListOf()
    private val images = mutableListOf<ImageAllocation>()

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

    fun allocateImage(createInfo: Image.CreateInfo): ImageAllocation {
        MemoryStack.stackPush().use { stack ->
            val width = createInfo.width
            val height = createInfo.height

            val imageInfo = VkImageCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                imageType(VK_IMAGE_TYPE_2D)
                extent().width(width)
                extent().height(height)
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

            val allocationInfo = VmaAllocationCreateInfo.calloc(stack).apply {
                usage(Vma.VMA_MEMORY_USAGE_AUTO)
            }

            val pImage = stack.mallocLong(1)
            val pAllocation = stack.mallocPointer(1)

            Vma.vmaCreateImage(allocator.handle.value, imageInfo, allocationInfo, pImage, pAllocation, null)
                .validateVulkanSuccess("Create image", "Failed to create image")

            val image = VkImage(pImage[0])
            val memory = VmaAllocation(pAllocation[0])

            return ImageAllocation(allocator, image, memory, width, height).also { imageAllocation ->
                images.add(imageAllocation)
                logger.info("Allocated image $image size ${width}x$height with memory $memory")
            }
        }
    }

    /**
     * Uploads image data to GPU memory using a staging buffer and performs necessary layout transitions.
     *
     * @param allocation The image allocation to upload data to
     * @param data The raw image data in ByteBuffer format to be uploaded
     * @return VulkanTexture object containing the uploaded image and associated resources
     */
    fun uploadImage(allocation: ImageAllocation, data: ByteBuffer) =
        bufferManager.withStagingBuffer(data) { stagingBuffer, fence ->
            val format = VK_FORMAT_R8G8B8A8_SRGB

            // ... converting to optimal layout to transfer to this device memory
            transitionImageLayout(
                allocation.image,
                format,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            )
            // ... copying the data from the staging buffer to the device memory
            copyBufferToImage(
                fence,
                stagingBuffer.buffer,
                allocation.image,
                allocation.width,
                allocation.height,
            )
            // ... converting to read only optimal layout
            transitionImageLayout(
                allocation.image,
                format,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
            )

            val textureImageView = ImageView(
                logicalDevice,
                allocation.image,
                ImageView.CreateInfo(
                    format = format,
                    aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                ),
            )

            VulkanTexture(
                allocation.image,
                allocation.memory,
                textureImageView,
            ).also { image ->
                textureImages += image
            }
        }

    private fun transitionImageLayout(image: VkImage, format: Int, oldLayout: Int, newLayout: Int) {
        // TODO : use dedicated transfer queue for this
        commandPool.recordSingleTimeCommands(logicalDevice.graphicsQueue) { stack, commandBuffer ->
            var sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
            var destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT

            val barrier = VkImageMemoryBarrier.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                oldLayout(oldLayout)
                newLayout(newLayout)
                srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                image(image.value)
                subresourceRange { range ->
                    range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    range.baseMipLevel(0)
                    range.levelCount(1)
                    range.baseArrayLayer(0)
                    range.layerCount(1)
                }

                if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                    srcAccessMask(0)
                    dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                    destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
                    newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                ) {
                    srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                    destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                } else {
                    error("Unsupported layout transition")
                }
            }

            val barriers = VkImageMemoryBarrier.calloc(1, stack)
                .put(barrier)
                .flip()

            vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barriers)
        }
    }

    fun copyBufferToImage(fence: Fence, buffer: VkBuffer, image: VkImage, width: Int, height: Int) {
        // TODO : use dedicated transfer queue for this
        commandPool.recordSingleTimeCommands(logicalDevice.graphicsQueue, fence) { stack, commandBuffer ->
            val region = VkBufferImageCopy.calloc(stack).apply {
                bufferOffset(0)
                bufferRowLength(0)
                bufferImageHeight(0)
                imageSubresource { subresource ->
                    subresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    subresource.mipLevel(0)
                    subresource.baseArrayLayer(0)
                    subresource.layerCount(1)
                }
                imageOffset { offset ->
                    offset.x(0)
                    offset.y(0)
                    offset.z(0)
                }
                imageExtent { extent ->
                    extent.width(width)
                    extent.height(height)
                    extent.depth(1)
                }
            }

            val regions = VkBufferImageCopy.calloc(1, stack)
                .put(region)
                .flip()

            vkCmdCopyBufferToImage(
                commandBuffer,
                buffer.value,
                image.value,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                regions,
            )
        }
    }

    override fun destroy() {
        vkDestroySampler(logicalDevice.handle, sampler.value, null)

        for (image in textureImages) {
            image.textureImageView.close()
        }

        images.forEach { image ->
            image.close()
        }
    }

    companion object {

        @JvmStatic
        private val logger = logger<TextureManager>()
    }
}
