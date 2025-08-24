package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.handle.VkPipelineLayout

class PipelineBuilder(
    val logicalDevice: LogicalDevice,
    val renderPass: RenderPass,
    val pipelineLayout: VkPipelineLayout,
) {

    var shaderModules: MutableMap<ShaderStage, ShaderModule> = mutableMapOf()
    var vertexLayout: VertexLayout<*> = TexturedVertex3

    fun build(): Pipeline = PipelineFactory.createPipeline(this)
}
