package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass

/**
 * Builder class for creating Vulkan graphics pipelines.
 * Configures and assembles all the components needed for a complete graphics pipeline.
 *
 * @property logicalDevice The logical device used to create the pipeline
 * @property renderPass The render pass that this pipeline will be compatible with
 * @property pipelineLayout The pipeline layout defining shader interfaces and resources
 */
class PipelineBuilder(
    val logicalDevice: LogicalDevice,
    val renderPass: RenderPass,
    val pipelineLayout: VkPipelineLayout,
) {

    /** Map of shader stages to their corresponding shader modules */
    var shaderModules: MutableMap<ShaderStage, ShaderModule> = mutableMapOf()

    /** Layout description for vertex input data */
    var vertexLayout: VertexLayout<*> = TexturedVertex3

    /**
     * Creates a new pipeline using the configured settings.
     *
     * @return The created pipeline object
     */
    fun build(): Pipeline = PipelineFactory.createPipeline(this)
}
