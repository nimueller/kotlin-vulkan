package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.image.Image
import dev.cryptospace.anvil.vulkan.buffer.BufferAllocation

data class VulkanImage(
    val imageBuffer: BufferAllocation,
) : Image
