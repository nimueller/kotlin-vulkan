package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.vulkan.utils.getVertexBindingDescription
import dev.cryptospace.anvil.vulkan.utils.toAttributeDescriptions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputBindingDescription

private const val SHADER_MAIN_FUNCTION_NAME = "main"

/**
 * Factory object for creating Vulkan graphics pipelines.
 *
 * This factory handles the creation and configuration of Vulkan graphics pipelines,
 * including setup of all pipeline states like vertex input, rasterization,
 * color blending, and depth testing.
 */
object PipelineFactory {

    /**
     * Creates a new Vulkan graphics pipeline using the provided configuration.
     *
     * Sets up all pipeline states and creates a new graphics pipeline object using
     * the specifications provided in the pipeline builder.
     *
     * @param pipelineBuilder Builder object containing pipeline configuration
     * @return Configured Pipeline object
     */
    fun createPipeline(pipelineBuilder: PipelineBuilder): Pipeline = MemoryStack.stackPush().use { stack ->
        val shaderStages = createShaderStageCreateInfo(stack, pipelineBuilder)
        val vertexInputInfo = setupVertexShaderInputInfo(stack, pipelineBuilder.vertexLayout)
        val dynamicState = setupDynamicState(stack)
        val inputAssembly = setupVertexInputAssembly(stack)
        val viewportState = setupViewport(stack)
        val rasterizer = setupRasterizer(stack, pipelineBuilder)
        val multisampling = setupMultisampling(stack)
        val depthStencilState = setupDepthStencilState(stack)
        val colorBlending = setupColorBlending(stack)

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            stageCount(shaderStages.remaining())
            pStages(shaderStages)
            pVertexInputState(vertexInputInfo)
            pInputAssemblyState(inputAssembly)
            pViewportState(viewportState)
            pRasterizationState(rasterizer)
            pMultisampleState(multisampling)
            pDepthStencilState(depthStencilState)
            pColorBlendState(colorBlending)
            pDynamicState(dynamicState)
            layout(pipelineBuilder.pipelineLayout.value)
            renderPass(pipelineBuilder.renderPass.handle.value)
            subpass(0)
            basePipelineHandle(VK10.VK_NULL_HANDLE)
            basePipelineIndex(-1)
        }
        val pipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            .put(pipelineCreateInfo)
            .flip()

        val pPipelines = stack.mallocLong(1)
        VK10.vkCreateGraphicsPipelines(
            pipelineBuilder.logicalDevice.handle,
            VK10.VK_NULL_HANDLE,
            pipelineCreateInfos,
            null,
            pPipelines,
        )
        return Pipeline(
            logicalDevice = pipelineBuilder.logicalDevice,
            pipelineLayoutHandle = pipelineBuilder.pipelineLayout,
            handle = VkPipeline(pPipelines[0]),
        )
    }

    /**
     * Sets up the color blending state for the pipeline.
     *
     * Configures how color outputs from the fragment shader are combined with existing framebuffer colors.
     * The current configuration:
     * - Disables blending (color output directly overwrites framebuffer)
     * - Enables all color component writes (R, G, B, A)
     * - Uses VK_BLEND_OP_ADD with VK_BLEND_FACTOR_ONE and VK_BLEND_FACTOR_ZERO
     * - Disables logical operations
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Color blend state configuration
     */
    private fun setupColorBlending(stack: MemoryStack): VkPipelineColorBlendStateCreateInfo {
        val colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(stack).apply {
            colorWriteMask(
                VK10.VK_COLOR_COMPONENT_R_BIT
                    or VK10.VK_COLOR_COMPONENT_G_BIT
                    or VK10.VK_COLOR_COMPONENT_B_BIT
                    or VK10.VK_COLOR_COMPONENT_A_BIT,
            )
            blendEnable(false)
            srcColorBlendFactor(VK10.VK_BLEND_FACTOR_ONE)
            dstColorBlendFactor(VK10.VK_BLEND_FACTOR_ZERO)
            colorBlendOp(VK10.VK_BLEND_OP_ADD)
            srcAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE)
            dstAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ZERO)
            alphaBlendOp(VK10.VK_BLEND_OP_ADD)
        }
        val colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            .put(colorBlendAttachment)
            .flip()

        return VkPipelineColorBlendStateCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            logicOpEnable(false)
            logicOp(VK10.VK_LOGIC_OP_COPY)
            attachmentCount(colorBlendAttachments.remaining())
            pAttachments(colorBlendAttachments)
            blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f))
        }
    }

    /**
     * Configures vertex input state for the pipeline.
     *
     * Sets up the format and organization of vertex data that will be provided to the vertex shader.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Vertex input state configuration
     */
    private fun setupVertexShaderInputInfo(
        stack: MemoryStack,
        vertexLayout: VertexLayout<*>,
    ): VkPipelineVertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack).apply {
        val bindingDescriptions = VkVertexInputBindingDescription.calloc(1, stack)
            .put(vertexLayout.getVertexBindingDescription(stack))
            .flip()
        val attributeDescriptions = vertexLayout.toAttributeDescriptions(stack)
        sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
        pVertexBindingDescriptions(bindingDescriptions)
        pVertexAttributeDescriptions(attributeDescriptions)
    }

    /**
     * Configures which pipeline states can be dynamically changed.
     *
     * Specifies pipeline states that can be modified without recreating the pipeline.
     * Currently, enables dynamic viewport and scissor states.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Dynamic state configuration
     */
    private fun setupDynamicState(stack: MemoryStack): VkPipelineDynamicStateCreateInfo =
        VkPipelineDynamicStateCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            pDynamicStates(stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR))
        }

    /**
     * Sets up how vertices should be assembled into primitives.
     *
     * Configures the primitive topology (triangle list) and primitive restart settings.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Input assembly state configuration
     */
    private fun setupVertexInputAssembly(stack: MemoryStack): VkPipelineInputAssemblyStateCreateInfo =
        VkPipelineInputAssemblyStateCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            primitiveRestartEnable(false)
        }

    /**
     * Configures viewport and scissor settings for the pipeline.
     *
     * Sets up the number of viewports and scissors to be used.
     * Actual viewport and scissor rectangles are set dynamically.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Viewport state configuration
     */
    private fun setupViewport(stack: MemoryStack): VkPipelineViewportStateCreateInfo =
        VkPipelineViewportStateCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            viewportCount(1)
            scissorCount(1)
        }

    /**
     * Configures rasterization settings for the pipeline.
     *
     * Sets up polygon rasterization parameters including fill mode, culling,
     * front face orientation, and depth settings.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Rasterization state configuration
     */
    private fun setupRasterizer(
        stack: MemoryStack,
        pipelineBuilder: PipelineBuilder,
    ): VkPipelineRasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack).apply {
        sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
        depthClampEnable(false)
        rasterizerDiscardEnable(false)
        polygonMode(VK10.VK_POLYGON_MODE_FILL)
        lineWidth(1.0f)
        cullMode(pipelineBuilder.cullMode.vkValue)
        frontFace(pipelineBuilder.frontFace.vkValue)
        depthBiasEnable(false)
    }

    /**
     * Configures multisampling settings for the pipeline.
     *
     * Sets up antialiasing parameters including sample counts and masks.
     * Currently configured for no multisampling (1 sample per pixel).
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Multisampling state configuration
     */
    private fun setupMultisampling(stack: MemoryStack): VkPipelineMultisampleStateCreateInfo =
        VkPipelineMultisampleStateCreateInfo.calloc(stack).apply {
            sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            sampleShadingEnable(false)
            rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT)
            minSampleShading(1.0f)
            pSampleMask(null)
            alphaToCoverageEnable(false)
            alphaToOneEnable(false)
        }

    /**
     * Configures depth and stencil testing settings for the pipeline.
     *
     * Sets up depth testing, writing, and comparison operations as well as stencil testing parameters.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @param depthTest Whether depth testing should be enabled
     * @param depthWrite Whether depth writing should be enabled
     * @param depthCompareOpOnDepthTest Depth comparison operation to use when depth testing is enabled
     * @return Depth stencil state configuration
     */
    private fun setupDepthStencilState(
        stack: MemoryStack,
        depthTest: Boolean = true,
        depthWrite: Boolean = true,
        depthCompareOpOnDepthTest: Int = VK10.VK_COMPARE_OP_LESS_OR_EQUAL,
    ): VkPipelineDepthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack).apply {
        sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
        depthTestEnable(depthTest)
        depthWriteEnable(depthWrite)
        depthCompareOp(if (depthTest) depthCompareOpOnDepthTest else VK10.VK_COMPARE_OP_ALWAYS)
        depthBoundsTestEnable(false)
        stencilTestEnable(false)
        minDepthBounds(0.0f)
        maxDepthBounds(1.0f)
    }

    /**
     * Creates shader stage configuration for all shader stages in the pipeline.
     *
     * Configures the pipeline stages for each shader module provided in the pipeline builder.
     * Sets up shader entry points and module references for all shader stages (vertex, fragment, etc.).
     * All shaders use the same entry point name defined by [SHADER_MAIN_FUNCTION_NAME].
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @param pipelineBuilder Builder containing shader modules and their stages
     * @return Buffer containing shader stage configurations for all shaders
     */
    private fun createShaderStageCreateInfo(
        stack: MemoryStack,
        pipelineBuilder: PipelineBuilder,
    ): VkPipelineShaderStageCreateInfo.Buffer {
        val buffer = VkPipelineShaderStageCreateInfo.malloc(pipelineBuilder.shaderModules.size, stack)
        val shaderMainFunctionName = stack.UTF8(SHADER_MAIN_FUNCTION_NAME)

        for ((shaderStage, shaderModule) in pipelineBuilder.shaderModules) {
            buffer.put(
                VkPipelineShaderStageCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(shaderStage.value)
                    .module(shaderModule.handle.value)
                    .pName(shaderMainFunctionName),
            )
        }

        buffer.flip()
        return buffer
    }
}
