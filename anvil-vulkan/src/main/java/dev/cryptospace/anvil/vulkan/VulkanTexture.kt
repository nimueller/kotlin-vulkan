package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.image.Texture
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.image.Image
import dev.cryptospace.anvil.vulkan.image.ImageView

data class VulkanTexture(
    val textureImage: Image,
    val textureImageMemory: VkDeviceMemory,
    val textureImageView: ImageView,
) : Texture
