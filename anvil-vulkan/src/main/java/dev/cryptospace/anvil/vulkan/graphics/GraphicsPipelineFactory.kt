package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.getAttributeDescriptions
import dev.cryptospace.anvil.vulkan.getBindingDescription
import dev.cryptospace.anvil.vulkan.handle.VkPipeline
import dev.cryptospace.anvil.vulkan.handle.VkPipelineLayout
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE
import org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO
import org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT
import org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT
import org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR
import org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT
import org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE
import org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL
import org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST
import org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines
import org.lwjgl.vulkan.VK10.vkCreateShaderModule
import org.lwjgl.vulkan.VK10.vkDestroyShaderModule
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import org.lwjgl.vulkan.VkVertexInputBindingDescription

/**
 * Factory for creating and configuring Vulkan graphics pipelines.
 *
 * Handles the creation of graphics pipelines including shader setup, vertex input configuration,
 * rasterization settings, and other pipeline states.
 */
object GraphicsPipelineFactory {

    /**
     * Creates a new graphics pipeline with the specified configuration.
     *
     * @param logicalDevice The logical device to create the pipeline on
     * @param renderPass The render pass that this pipeline will be compatible with
     * @param pipelineLayoutHandle The pipeline layout to use
     * @return A new VkPipeline instance
     */
    fun createGraphicsPipeline(
        logicalDevice: LogicalDevice,
        renderPass: RenderPass,
        pipelineLayoutHandle: VkPipelineLayout,
    ): VkPipeline = MemoryStack.stackPush().use { stack ->
        val vertexShaderHandle = loadShader(logicalDevice, "/shaders/vert.spv")
        val fragmentShaderHandle = loadShader(logicalDevice, "/shaders/frag.spv")

        val vertexShaderCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage(VK_SHADER_STAGE_VERTEX_BIT)
            module(vertexShaderHandle.value)
            pName(stack.UTF8("main"))
        }
        val fragmentShaderCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            module(fragmentShaderHandle.value)
            pName(stack.UTF8("main"))
        }

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
            .put(vertexShaderCreateInfo)
            .put(fragmentShaderCreateInfo)
            .flip()

        val pipelineCreateInfos =
            createPipelineCreateInfoBuffer(stack, shaderStages, pipelineLayoutHandle, renderPass)

        val pPipelines = stack.mallocLong(1)
        vkCreateGraphicsPipelines(
            logicalDevice.handle,
            VK_NULL_HANDLE,
            pipelineCreateInfos,
            null,
            pPipelines,
        ).validateVulkanSuccess()

        vkDestroyShaderModule(logicalDevice.handle, vertexShaderHandle.value, null)
        vkDestroyShaderModule(logicalDevice.handle, fragmentShaderHandle.value, null)

        VkPipeline(pPipelines[0])
    }

    /**
     * Creates the pipeline configuration buffer with all necessary state information.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @param shaderStages Shader stages to be used in the pipeline
     * @param pipelineLayoutHandle Pipeline layout handle
     * @param renderPass Render pass configuration
     * @return Buffer containing pipeline creation information
     */
    private fun createPipelineCreateInfoBuffer(
        stack: MemoryStack,
        shaderStages: VkPipelineShaderStageCreateInfo.Buffer,
        pipelineLayoutHandle: VkPipelineLayout,
        renderPass: RenderPass,
    ): VkGraphicsPipelineCreateInfo.Buffer {
        val vertexInputInfo = setupVertexShaderInputInfo(stack)
        val dynamicState = setupDynamicState(stack)
        val inputAssembly = setupVertexInputAssembly(stack)
        val viewportState = setupViewport(stack)
        val rasterizer = setupRasterizer(stack)
        val multisampling = setupMultisampling(stack)
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
            pDepthStencilState(null)
            pColorBlendState(colorBlending)
            pDynamicState(dynamicState)
            layout(pipelineLayoutHandle.value)
            renderPass(renderPass.handle.value)
            subpass(0)
            basePipelineHandle(VK_NULL_HANDLE)
            basePipelineIndex(-1)
        }
        val pipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            .put(pipelineCreateInfo)
            .flip()
        return pipelineCreateInfos
    }

    /**
     * Sets up color blending state for the pipeline.
     *
     * Configures how color outputs from fragment shader are combined with existing framebuffer colors.
     * Currently sets up a basic configuration with blending disabled.
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
    private fun setupVertexShaderInputInfo(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo =
        VkPipelineVertexInputStateCreateInfo.calloc(stack).apply {
            val bindingDescriptions = VkVertexInputBindingDescription.calloc(1, stack)
                .put(getBindingDescription(stack))
                .flip()
            val attributeDescriptions = getAttributeDescriptions(stack)
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
            frontFace(VK_FRONT_FACE_CLOCKWISE)
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

    /**
     * Loads and creates a shader module from the specified resource path.
     *
     * @param logicalDevice The logical device to create the shader module on
     * @param path Resource path to the shader file
     * @return Handle to the created shader module
     * @throws IllegalStateException if shader file cannot be found
     */
    private fun loadShader(logicalDevice: LogicalDevice, path: String): Handle = MemoryStack.stackPush().use { stack ->
        val shaderCode = GraphicsPipelineFactory::class.java.getResourceAsStream(path)?.readAllBytes()
            ?: error("Shader not found at classpath: $path")
        val shaderCodeBuffer = stack.malloc(shaderCode.size)
        shaderCodeBuffer.put(shaderCode)
        shaderCodeBuffer.flip()

        val createInfo = VkShaderModuleCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
            pCode(shaderCodeBuffer)
        }

        val shaderModule = stack.mallocLong(1)
        vkCreateShaderModule(logicalDevice.handle, createInfo, null, shaderModule).validateVulkanSuccess()

        Handle(shaderModule[0])
    }
}
