package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSet
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo

/**
 * Represents a collection of Vulkan descriptor sets that bind resources to shader bindings.
 * Descriptor sets contain the actual binding information between shader resources
 * (like uniform buffers and samplers) and the shader binding points.
 *
 * @property logicalDevice The logical device this descriptor set belongs to
 * @property handles List of native Vulkan descriptor set handles
 */
class DescriptorSet(
    val handles: List<VkDescriptorSet>,
) {

    /**
     * Creates descriptor sets from the specified pool using the given layout.
     *
     * @param logicalDevice The logical device to create the descriptor sets on
     * @param pool The descriptor pool to allocate from
     * @param layout The layout defining the structure of the descriptor sets
     * @param count Number of descriptor sets to create
     */
    constructor(logicalDevice: LogicalDevice, pool: DescriptorPool, layout: DescriptorSetLayout, count: Int) : this(
        createDescriptorSet(logicalDevice, pool, layout, count),
    )

    operator fun get(index: Int): VkDescriptorSet = handles[index]

    companion object {

        /**
         * Creates multiple descriptor sets with the same layout.
         *
         * @param logicalDevice The logical device to create the descriptor sets on
         * @param pool The descriptor pool to allocate from
         * @param layout The layout defining the structure of the descriptor sets
         * @param count Number of descriptor sets to create
         * @return List of created descriptor set handles
         */
        private fun createDescriptorSet(
            logicalDevice: LogicalDevice,
            pool: DescriptorPool,
            layout: DescriptorSetLayout,
            count: Int,
        ): List<VkDescriptorSet> = MemoryStack.stackPush().use { stack ->
            check(count > 0) { "Count must be greater than 0" }

            val setLayouts = stack.mallocLong(count)

            repeat(count) {
                setLayouts.put(layout.handle.value)
            }

            setLayouts.flip()

            val allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                descriptorPool(pool.handle.value)
                pSetLayouts(setLayouts)
            }

            val pDescriptorSets = stack.mallocLong(count)
            vkAllocateDescriptorSets(logicalDevice.handle, allocateInfo, pDescriptorSets)
                .validateVulkanSuccess("Allocate descriptor sets", "Failed to allocate descriptor sets")
            List(count) {
                VkDescriptorSet(pDescriptorSets[it])
            }
        }
    }
}
