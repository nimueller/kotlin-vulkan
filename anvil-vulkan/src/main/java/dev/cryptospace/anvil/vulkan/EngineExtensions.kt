package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vertex2
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

fun getVertex2BindingDescription(stack: MemoryStack): VkVertexInputBindingDescription =
    VkVertexInputBindingDescription.calloc(stack)
        .binding(0)
        .stride(Vertex2.BYTE_SIZE)
        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

fun getVertex2AttributeDescriptions(stack: MemoryStack): VkVertexInputAttributeDescription.Buffer =
    VkVertexInputAttributeDescription.calloc(3, stack).also { buffer ->
        buffer
            .put(
                VkVertexInputAttributeDescription.calloc(stack).apply {
                    binding(0)
                    location(0)
                    format(VK_FORMAT_R32G32_SFLOAT)
                    offset(0)
                },
            )
            .put(
                VkVertexInputAttributeDescription.calloc(stack).apply {
                    binding(0)
                    location(1)
                    format(VK_FORMAT_R32G32B32_SFLOAT)
                    offset(Vec2.BYTE_SIZE)
                },
            )
            .put(
                VkVertexInputAttributeDescription.calloc(stack).apply {
                    binding(0)
                    location(2)
                    format(VK_FORMAT_R32G32_SFLOAT)
                    offset(Vec2.BYTE_SIZE + Vec3.BYTE_SIZE)
                },
            )
            .flip()
    }
