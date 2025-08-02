package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.handle.VkImage
import dev.cryptospace.anvil.vulkan.image.ImageView

data class VulkanImage(
    val textureImage: VkImage,
    val textureImageMemory: VkDeviceMemory,
    val textureImageView: ImageView,
) : Image
