package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding

/**
 * Represents a specialized Vulkan descriptor set layout specifically designed for frame-specific uniform buffers.
 *
 * This class creates a descriptor set layout with a single binding for a uniform buffer that is accessible
 * from the vertex shader stage. It is typically used to manage per-frame data such as view and projection
 * matrices that need to be updated each frame.
 *
 * The layout consists of:
 * - Binding 0: Uniform buffer (VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
 *   - Accessible from: Vertex shader stage only
 *   - Count: 1 buffer
 *
 * @property handle The native Vulkan handle to the descriptor set layout
 */
class FrameDescriptorSetLayout(
    logicalDevice: LogicalDevice,
) : DescriptorSetLayout(
    logicalDevice = logicalDevice,
    handle = MemoryStack.stackPush().use { stack ->
        createDescriptorSetLayout(logicalDevice, stack, listOf(createVertexShaderDescriptor(stack)))
    },
) {

    companion object {

        private fun createVertexShaderDescriptor(stack: MemoryStack): VkDescriptorSetLayoutBinding =
            VkDescriptorSetLayoutBinding.calloc(stack).apply {
                binding(0)
                descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                descriptorCount(1)
                stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                pImmutableSamplers(null)
            }
    }
}
