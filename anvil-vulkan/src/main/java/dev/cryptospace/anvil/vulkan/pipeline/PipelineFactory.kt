package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.vulkan.utils.getVertexBindingDescription
import dev.cryptospace.anvil.vulkan.utils.toAttributeDescriptions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE
import org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO
import org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT
import org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS
import org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS_OR_EQUAL
import org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT
import org.lwjgl.vulkan.VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE
import org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL
import org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO
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

object PipelineFactory {

    fun createPipeline(pipelineBuilder: PipelineBuilder): VkPipeline = MemoryStack.stackPush().use { stack ->
        val shaderStages = createShaderStageCreateInfo(stack, pipelineBuilder)
        val vertexInputInfo = setupVertexShaderInputInfo(stack, pipelineBuilder.vertexLayout)
        val dynamicState = setupDynamicState(stack)
        val inputAssembly = setupVertexInputAssembly(stack)
        val viewportState = setupViewport(stack)
        val rasterizer = setupRasterizer(stack)
        val multisampling = setupMultisampling(stack)
        val depthStencilState = setupDepthStencilState(stack)
        val colorBlending = setupColorBlending(stack)

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
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
            basePipelineHandle(VK_NULL_HANDLE)
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
        VkPipeline(pPipelines[0])
    }

    /**
     * Sets up color blending state for the pipeline.
     *
     * Configures how color outputs from fragment shader are combined with existing framebuffer colors.
     * The current configuration:
     * - Disables blending (color output directly overwrites framebuffer)
     * - Enables all color component writes (R,G,B,A)
     * - Uses VK_BLEND_OP_ADD with VK_BLEND_FACTOR_ONE and VK_BLEND_FACTOR_ZERO
     * - Disables logical operations
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Color blend state configuration
     */
    private fun setupColorBlending(stack: MemoryStack): VkPipelineColorBlendStateCreateInfo {
        val colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(stack).apply {
            colorWriteMask(
                VK_COLOR_COMPONENT_R_BIT
                    or VK_COLOR_COMPONENT_G_BIT
                    or VK_COLOR_COMPONENT_B_BIT
                    or VK_COLOR_COMPONENT_A_BIT,
            )
            blendEnable(false)
            srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
            dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
            colorBlendOp(VK_BLEND_OP_ADD)
            srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
            dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            alphaBlendOp(VK_BLEND_OP_ADD)
        }
        val colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            .put(colorBlendAttachment)
            .flip()

        return VkPipelineColorBlendStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            logicOpEnable(false)
            logicOp(VK_LOGIC_OP_COPY)
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
        sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
        pVertexBindingDescriptions(bindingDescriptions)
        pVertexAttributeDescriptions(attributeDescriptions)
    }

    /**
     * Configures which pipeline states can be dynamically changed.
     *
     * Specifies pipeline states that can be modified without recreating the pipeline.
     * Currently enables dynamic viewport and scissor states.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Dynamic state configuration
     */
    private fun setupDynamicState(stack: MemoryStack): VkPipelineDynamicStateCreateInfo =
        VkPipelineDynamicStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))
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
            sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
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
            sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
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
    private fun setupRasterizer(stack: MemoryStack): VkPipelineRasterizationStateCreateInfo =
        VkPipelineRasterizationStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            depthClampEnable(false)
            rasterizerDiscardEnable(false)
            polygonMode(VK_POLYGON_MODE_FILL)
            lineWidth(1.0f)
            cullMode(VK_CULL_MODE_BACK_BIT)
            frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
            depthBiasEnable(false)
        }

    /**
     * Configures multisampling settings for the pipeline.
     *
     * Sets up anti-aliasing parameters including sample counts and masks.
     * Currently configured for no multisampling (1 sample per pixel).
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @return Multisampling state configuration
     */
    private fun setupMultisampling(stack: MemoryStack): VkPipelineMultisampleStateCreateInfo =
        VkPipelineMultisampleStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            sampleShadingEnable(false)
            rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
            minSampleShading(1.0f)
            pSampleMask(null)
            alphaToCoverageEnable(false)
            alphaToOneEnable(false)
        }

    private fun setupDepthStencilState(
        stack: MemoryStack,
        depthTest: Boolean = true,
        depthWrite: Boolean = true,
        depthCompareOpOnDepthTest: Int = VK_COMPARE_OP_LESS_OR_EQUAL,
    ): VkPipelineDepthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack).apply {
        sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
        depthTestEnable(depthTest)
        depthWriteEnable(depthWrite)
        depthCompareOp(if (depthTest) depthCompareOpOnDepthTest else VK_COMPARE_OP_ALWAYS)
        depthBoundsTestEnable(false)
        stencilTestEnable(false)
        minDepthBounds(0.0f)
        maxDepthBounds(1.0f)
    }

    /**
     * Creates shader stage configuration for vertex and fragment shaders.
     *
     * Configures the pipeline stages for both vertex and fragment shaders,
     * setting up their entry points and shader module references.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @param vertexShader Handle to the compiled vertex shader module
     * @param fragmentShader Handle to the compiled fragment shader module
     * @return Buffer containing shader stage configurations for both shaders
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
