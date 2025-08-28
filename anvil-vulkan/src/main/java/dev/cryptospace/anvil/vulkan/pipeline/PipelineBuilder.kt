package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderManager
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderModule
import org.lwjgl.vulkan.VK10

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
    val shaderManager: ShaderManager,
) {

    /** Map of shader stages to their corresponding shader modules */
    var shaderModules: MutableMap<ShaderType, ShaderModule> = mutableMapOf()

    /** Layout description for vertex input data */
    var vertexLayout: VertexLayout<*> = TexturedVertex3

    /**
     * Specifies the face culling mode for the pipeline.
     * Determines which polygon faces should be culled during rendering.
     * Defaults to BACK face culling for more efficient rendering.
     */
    var cullMode: CullMode = CullMode.BACK

    /**
     * Specifies the winding order that determines which side of a polygon is considered the front face.
     * Used in conjunction with cullMode to determine which faces should be culled.
     * Defaults to COUNTER_CLOCKWISE winding order.
     */
    var frontFace: FrontFace = FrontFace.COUNTER_CLOCKWISE

    fun shaderModule(type: ShaderType, shaderId: ShaderId) {
        shaderModules[type] = shaderManager.getRegisteredShader(shaderId).shaderModule
    }

    /**
     * Creates a new pipeline using the configured settings.
     *
     * @return The created pipeline object
     */
    fun build(): Pipeline = PipelineFactory.createPipeline(this)

    /**
     * Defines face culling modes for the graphics pipeline.
     * Controls which polygon faces are discarded during rendering to improve performance.
     *
     * @property vkValue The Vulkan enum value corresponding to this culling mode
     */
    enum class CullMode(
        val vkValue: Int,
    ) {
        /** Disables face culling - all faces are rendered */
        NONE(VK10.VK_CULL_MODE_NONE),

        /** Culls front-facing polygons */
        FRONT(VK10.VK_CULL_MODE_FRONT_BIT),

        /** Culls back-facing polygons */
        BACK(VK10.VK_CULL_MODE_BACK_BIT),

        /** Culls both front and back-facing polygons */
        FRONT_AND_BACK(VK10.VK_STENCIL_FRONT_AND_BACK),
    }

    /**
     * Defines the winding order for determining front-facing polygons in the graphics pipeline.
     * This affects how face culling is applied based on vertex ordering.
     *
     * @property vkValue The Vulkan enum value corresponding to this front face mode
     */
    enum class FrontFace(
        val vkValue: Int,
    ) {
        /** Specifies that polygons with counter-clockwise vertex ordering are considered front-facing */
        COUNTER_CLOCKWISE(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE),

        /** Specifies that polygons with clockwise vertex ordering are considered front-facing */
        CLOCKWISE(VK10.VK_FRONT_FACE_CLOCKWISE),
    }
}
