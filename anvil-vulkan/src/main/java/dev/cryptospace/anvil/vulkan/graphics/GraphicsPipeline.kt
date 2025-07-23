package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkPipeline
import dev.cryptospace.anvil.vulkan.handle.VkPipelineLayout
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreatePipelineLayout
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo

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
     * Creates a graphics pipeline with an automatically generated pipeline layout.
     *
     * @param logicalDevice The logical device to create the pipeline on
     * @param renderPass The render pass this pipeline will be compatible with
     */
    constructor(logicalDevice: LogicalDevice, renderPass: RenderPass) : this(
        logicalDevice,
        renderPass,
        createGraphicsPipelineLayout(logicalDevice),
    )

    /**
     * Creates a graphics pipeline with a provided pipeline layout.
     *
     * @param logicalDevice The logical device to create the pipeline on
     * @param renderPass The render pass this pipeline will be compatible with
     * @param pipelineLayoutHandle The pipeline layout to use for this pipeline
     */
    constructor(logicalDevice: LogicalDevice, renderPass: RenderPass, pipelineLayoutHandle: VkPipelineLayout) : this(
        logicalDevice,
        pipelineLayoutHandle,
        GraphicsPipelineFactory.createGraphicsPipeline(logicalDevice, renderPass, pipelineLayoutHandle)
            .also { graphicsPipeline ->
                logger.debug { "Created graphics pipeline: $graphicsPipeline" }
            },
    )

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
        private fun createGraphicsPipelineLayout(logicalDevice: LogicalDevice): VkPipelineLayout =
            MemoryStack.stackPush().use { stack ->
                val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    pSetLayouts(null)
                    pPushConstantRanges(null)
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
