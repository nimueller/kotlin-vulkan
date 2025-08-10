package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.image.Texture
import dev.cryptospace.anvil.vulkan.buffer.VmaAllocation
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.image.ImageView

data class VulkanTexture(
    val textureImage: VkImage,
    val textureImageMemory: VmaAllocation,
    val textureImageView: ImageView,
) : Texture
