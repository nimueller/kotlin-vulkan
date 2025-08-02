package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.math.AttributeFormat
import dev.cryptospace.anvil.core.math.VertexLayout
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

fun AttributeFormat.toVulkanFormat(): Int = when (this) {
    AttributeFormat.SIGNED_FLOAT_2D -> VK_FORMAT_R32G32_SFLOAT
    AttributeFormat.SIGNED_FLOAT_3D -> VK_FORMAT_R32G32B32_SFLOAT
}

fun VertexLayout<*>.getVertexBindingDescription(stack: MemoryStack): VkVertexInputBindingDescription =
    VkVertexInputBindingDescription.calloc(stack)
        .binding(0)
        .stride(byteSize)
        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

fun VertexLayout<*>.toAttributeDescriptions(stack: MemoryStack): VkVertexInputAttributeDescription.Buffer {
    val descriptions = getAttributeDescriptions(0)
    val buffer = VkVertexInputAttributeDescription.calloc(descriptions.size, stack)

    for (description in descriptions) {
        buffer.put(
            VkVertexInputAttributeDescription.calloc(stack).apply {
                binding(description.binding)
                location(description.location)
                format(description.format.toVulkanFormat())
                offset(description.offset)
            },
        )
    }

    buffer.flip()
    return buffer
}
