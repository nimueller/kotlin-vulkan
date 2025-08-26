package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderStage.Companion.toBitmask
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfo
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

/**
 * Factory object responsible for creating Vulkan descriptor set layouts.
 *
 * This factory handles the creation of VkDescriptorSetLayout objects by processing
 * descriptor set specifications defined through the DescriptorSetBuilder.
 * It manages the configuration of descriptor bindings and their associated flags.
 */
object DescriptorSetLayoutFactory {

    /**
     * Creates a new descriptor set layout based on the provided builder configuration.
     *
     * @param logicalDevice The logical device used to create the descriptor set layout
     * @param descriptorSetBuilder The builder containing the descriptor set specifications
     * @return A new [DescriptorSetLayout] instance configured according to the builder
     */
    fun createDescriptorSetLayout(
        logicalDevice: LogicalDevice,
        descriptorSetBuilder: DescriptorSetBuilder,
    ): DescriptorSetLayout = MemoryStack.stackPush().use { stack ->
        val bindings = createBindings(stack, descriptorSetBuilder)

        val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            pBindings(bindings)

            if (descriptorSetBuilder.lastBindingHasVariableDescriptorCount) {
                pNext(createFlagsInfo(stack))
                flags(VK12.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT)
            }
        }

        val pSetLayout = stack.mallocLong(1)
        VK10.vkCreateDescriptorSetLayout(
            logicalDevice.handle,
            createInfo,
            null,
            pSetLayout,
        ).validateVulkanSuccess("Create descriptor set layout", "Failed to create descriptor set layout")

        return DescriptorSetLayout(
            logicalDevice = logicalDevice,
            handle = VkDescriptorSetLayout(pSetLayout[0]),
        )
    }

    /**
     * Creates descriptor set layout bindings from the builder configuration.
     *
     * @param stack The memory stack used for native memory allocation
     * @param descriptorSetBuilder The builder containing binding specifications
     * @return A buffer containing the created descriptor set layout bindings
     */
    private fun createBindings(
        stack: MemoryStack,
        descriptorSetBuilder: DescriptorSetBuilder,
    ): VkDescriptorSetLayoutBinding.Buffer {
        val buffer = VkDescriptorSetLayoutBinding.calloc(descriptorSetBuilder.bindings.size, stack)

        for ((bindingIndex, binding) in descriptorSetBuilder.bindings) {
            val stageFlags = binding.stages.toBitmask()
            buffer.put(
                VkDescriptorSetLayoutBinding.calloc(stack)
                    .binding(bindingIndex)
                    .descriptorType(binding.descriptorType.vkValue)
                    .descriptorCount(binding.descriptorCount)
                    .stageFlags(stageFlags),
            )
        }

        buffer.flip()
        return buffer
    }

    /**
     * Creates binding flags information structure for variable descriptor count support.
     *
     * @param stack The memory stack used for native memory allocation
     * @return The configured binding flags create info structure
     */
    private fun createFlagsInfo(stack: MemoryStack) = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack).apply {
        sType(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
        bindingCount(1)
        pBindingFlags(
            stack.ints(
                VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT or
                    VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT or
                    VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT,
            ),
        )
    }
}
