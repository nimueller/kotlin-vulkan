package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import org.lwjgl.vulkan.VkDescriptorSetVariableDescriptorCountAllocateInfo
import java.nio.LongBuffer

/**
 * Factory object responsible for creating Vulkan descriptor sets.
 *
 * This factory handles the allocation and configuration of descriptor sets, including support for
 * variable descriptor counts. It works in conjunction with the DescriptorSetBuilder to create
 * properly configured descriptor sets based on the specified layout and pool.
 */
object DescriptorSetFactory {

    /**
     * Creates a new descriptor set based on the provided configuration.
     *
     * @param logicalDevice The logical device used to create the descriptor set
     * @param descriptorSetBuilder The builder containing the descriptor set configuration
     * @param descriptorPool The descriptor pool from which to allocate the descriptor set
     * @param descriptorSetLayout The layout defining the structure of the descriptor set
     * @param setCount The number of descriptor sets to allocate
     * @return A new [DescriptorSet] instance
     * @throws IllegalStateException if setCount is less than or equal to 0
     */
    fun createDescriptorSet(
        logicalDevice: LogicalDevice,
        descriptorSetBuilder: DescriptorSetBuilder,
        descriptorPool: DescriptorPool,
        descriptorSetLayout: DescriptorSetLayout,
        setCount: Int,
    ): DescriptorSet = MemoryStack.stackPush().use { stack ->
        check(setCount > 0) { "Count must be greater than 0" }

        val setLayouts = stack.mallocLong(setCount)

        repeat(setCount) {
            setLayouts.put(descriptorSetLayout.handle.value)
        }

        setLayouts.flip()

        val allocateInfo = createAllocateInfo(stack, descriptorPool, setLayouts, descriptorSetBuilder, setCount)

        val pDescriptorSets = stack.mallocLong(setCount)
        VK10.vkAllocateDescriptorSets(
            logicalDevice.handle,
            allocateInfo,
            pDescriptorSets,
        ).validateVulkanSuccess("Allocate descriptor sets", "Failed to allocate descriptor sets")

        DescriptorSet(
            handles = List(setCount) {
                VkDescriptorSet(pDescriptorSets[it])
            },
        )
    }

    /**
     * Creates the allocation info structure for descriptor set allocation.
     *
     * @param stack The memory stack used for allocation
     * @param descriptorPool The descriptor pool to allocate from
     * @param setLayouts Buffer containing the descriptor set layouts
     * @param descriptorSetBuilder The builder containing a descriptor set configuration
     * @param setCount The number of descriptor sets to allocate
     * @return Configured [VkDescriptorSetAllocateInfo] structure
     */
    private fun createAllocateInfo(
        stack: MemoryStack,
        descriptorPool: DescriptorPool,
        setLayouts: LongBuffer,
        descriptorSetBuilder: DescriptorSetBuilder,
        setCount: Int,
    ): VkDescriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).apply {
        sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
        descriptorPool(descriptorPool.handle.value)
        pSetLayouts(setLayouts)

        if (descriptorSetBuilder.lastBindingHasVariableDescriptorCount) {
            setVariableDescriptorCount(descriptorSetBuilder, stack, setCount)
        }
    }

    /**
     * Configures variable descriptor count information for the allocation info.
     *
     * @param descriptorSetBuilder The builder containing the binding configuration
     * @param stack The memory stack used for allocation
     * @param setCount The number of descriptor sets being allocated
     * @throws IllegalStateException if no bindings are found or if the last binding has no descriptor count
     */
    private fun VkDescriptorSetAllocateInfo.setVariableDescriptorCount(
        descriptorSetBuilder: DescriptorSetBuilder,
        stack: MemoryStack,
        setCount: Int,
    ) {
        val lastBindingIndex = descriptorSetBuilder.bindings.keys.maxOrNull() ?: error("No bindings found")
        val totalDescriptorCount = descriptorSetBuilder.bindings[lastBindingIndex]?.descriptorCount
            ?: error("No descriptor count found for binding $lastBindingIndex")

        val descriptorCounts = stack.mallocInt(setCount)

        repeat(setCount) {
            descriptorCounts.put(totalDescriptorCount)
        }

        descriptorCounts.flip()

        val countInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack).apply {
            sType(VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO)
            pDescriptorCounts(descriptorCounts)
        }
        pNext(countInfo)
    }
}
