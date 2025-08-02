package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.handle.VkImage

data class VulkanImage(
    val textureImage: VkImage,
    val textureImageMemory: VkDeviceMemory,
) : Image
