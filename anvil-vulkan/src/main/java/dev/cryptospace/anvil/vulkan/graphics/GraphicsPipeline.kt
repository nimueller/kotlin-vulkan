package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.descriptor.DescriptorSetLayout
import dev.cryptospace.anvil.vulkan.handle.VkPipelineLayout
import dev.cryptospace.anvil.vulkan.pipeline.VkPipeline
import dev.cryptospace.anvil.vulkan.utils.toBuffer
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreatePipelineLayout
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange

/**
 * Represents a Vulkan graphics pipeline that defines the rendering state and shader stages.
 *
 * The graphics pipeline encapsulates the complete state needed for rendering operations,
 * including shaders, vertex layouts, and other fixed-function state.
 */
data class GraphicsPipeline(
    /** The logical device that this pipeline is associated with */
    val logicalDevice: LogicalDevice,
    /** Handle to the pipeline layout defining descriptor set layouts and push constants */
    val pipelineLayoutHandle: VkPipelineLayout,
    /** Native handle to the Vulkan graphics pipeline object */
    val handle: VkPipeline,
) : NativeResource() {

    /**
     * Destroys the graphics pipeline and its associated pipeline layout.
     * This releases all Vulkan resources associated with this pipeline.
     */
    override fun destroy() {
        vkDestroyPipeline(logicalDevice.handle, handle.value, null)
        vkDestroyPipelineLayout(logicalDevice.handle, pipelineLayoutHandle.value, null)
    }

    companion object {

        @JvmStatic
        private val logger = logger<GraphicsPipeline>()

        /**
         * Creates a basic pipeline layout without descriptor sets or push constants.
         *
         * @param logicalDevice The logical device to create the layout on
         * @return Handle to the created pipeline layout
         */
        fun createGraphicsPipelineLayout(
            logicalDevice: LogicalDevice,
            descriptorSetLayout: List<DescriptorSetLayout>,
        ): VkPipelineLayout = MemoryStack.stackPush().use { stack ->
            val setLayouts = stack.callocLong(descriptorSetLayout.size)

            descriptorSetLayout.forEach { descriptorSetLayout ->
                setLayouts.put(descriptorSetLayout.handle.value)
            }

            setLayouts.flip()

            val pushConstantRanges = listOf(
                VkPushConstantRange.calloc(stack)
                    .stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(Mat4.BYTE_SIZE),
                VkPushConstantRange.calloc(stack)
                    .stageFlags(VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                    .offset(Mat4.BYTE_SIZE)
                    .size(Int.SIZE_BYTES),
            ).toBuffer { size ->
                VkPushConstantRange.calloc(size, stack)
            }

            val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                pSetLayouts(setLayouts)
                pPushConstantRanges(pushConstantRanges)
            }

            val pPipelineLayout = stack.mallocLong(1)

            vkCreatePipelineLayout(
                logicalDevice.handle,
                pipelineLayoutCreateInfo,
                null,
                pPipelineLayout,
            ).validateVulkanSuccess()

            VkPipelineLayout(pPipelineLayout[0])
        }
    }
}
