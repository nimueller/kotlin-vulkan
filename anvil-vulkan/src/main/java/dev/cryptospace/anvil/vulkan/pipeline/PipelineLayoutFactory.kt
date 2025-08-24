package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import java.nio.LongBuffer

/**
 * Factory object responsible for creating Vulkan pipeline layouts.
 * Handles the creation and configuration of pipeline layouts with support for push constants and descriptor sets.
 */
object PipelineLayoutFactory {

    /**
     * Creates a new Vulkan pipeline layout using the provided builder configuration.
     *
     * @param pipelineLayoutBuilder The builder containing pipeline layout configuration
     * @return Handle to the created pipeline layout
     */
    fun createPipelineLayout(pipelineLayoutBuilder: PipelineLayoutBuilder): VkPipelineLayout =
        MemoryStack.stackPush().use { stack ->
            val pushConstantRanges = createPushConstantRanges(stack, pipelineLayoutBuilder)
            val descriptorSetLayouts = createDescriptorSetLayouts(stack, pipelineLayoutBuilder)

            val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                pSetLayouts(descriptorSetLayouts)
                pPushConstantRanges(pushConstantRanges)
            }

            val pPipelineLayout = stack.mallocLong(1)
            VK10.vkCreatePipelineLayout(
                pipelineLayoutBuilder.logicalDevice.handle,
                pipelineLayoutCreateInfo,
                null,
                pPipelineLayout,
            ).validateVulkanSuccess("Create pipeline layout", "Failed to create pipeline layout")

            VkPipelineLayout(pPipelineLayout[0])
        }

    /**
     * Creates push constant ranges buffer from the pipeline layout configuration.
     *
     * @param stack Memory stack for temporary allocations
     * @param pipelineLayoutBuilder The builder containing push constant ranges
     * @return Buffer containing the configured push constant ranges
     */
    private fun createPushConstantRanges(
        stack: MemoryStack,
        pipelineLayoutBuilder: PipelineLayoutBuilder,
    ): VkPushConstantRange.Buffer {
        val buffer = VkPushConstantRange.malloc(pipelineLayoutBuilder.pushConstantRanges.size, stack)

        for (range in pipelineLayoutBuilder.pushConstantRanges) {
            buffer.put(
                VkPushConstantRange.calloc(stack)
                    .stageFlags(range.stages.map { stage -> stage.value }.reduce { acc, bit -> acc or bit })
                    .offset(range.offset)
                    .size(range.size),
            )
        }

        buffer.flip()
        return buffer
    }

    /**
     * Creates a buffer containing descriptor set layout handles from the pipeline configuration.
     *
     * @param stack Memory stack for temporary allocations
     * @param pipelineLayoutBuilder The builder containing descriptor set layouts
     * @return Buffer containing the descriptor set layout handles
     */
    fun createDescriptorSetLayouts(stack: MemoryStack, pipelineLayoutBuilder: PipelineLayoutBuilder): LongBuffer {
        val setLayouts = stack.callocLong(pipelineLayoutBuilder.descriptorSetLayouts.size)

        for (layout in pipelineLayoutBuilder.descriptorSetLayouts) {
            setLayouts.put(layout.handle.value)
        }

        setLayouts.flip()
        return setLayouts
    }
}
