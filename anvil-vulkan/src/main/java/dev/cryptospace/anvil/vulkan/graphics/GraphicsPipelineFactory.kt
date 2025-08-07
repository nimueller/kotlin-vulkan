package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.getVertexBindingDescription
import dev.cryptospace.anvil.vulkan.handle.VkPipeline
import dev.cryptospace.anvil.vulkan.handle.VkPipelineLayout
import dev.cryptospace.anvil.vulkan.toAttributeDescriptions
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

/**
 * Factory responsible for creating and configuring Vulkan graphics pipelines.
 *
 * This factory handles the complete pipeline creation process including:
 * - Loading and configuring vertex and fragment shaders
 * - Setting up vertex input and attribute descriptions
 * - Configuring fixed-function pipeline states (rasterization, blending, etc.)
 * - Managing pipeline resources and memory
 *
 * The created pipelines are optimized for 2D rendering with basic texture mapping
 * and use dynamic viewport/scissor states for flexibility.
 */
object GraphicsPipelineFactory {

    /**
     * Creates a new Vulkan graphics pipeline with the specified configuration.
     *
     * This method performs the following steps:
     * 1. Loads and creates shader modules for vertex and fragment shaders
     * 2. Sets up all pipeline states (vertex input, assembly, rasterization, etc.)
     * 3. Creates the pipeline using the configured settings
     * 4. Cleans up temporary resources (shader modules)
     *
     * @param logicalDevice The logical device to create the pipeline on
     * @param renderPass The render pass that this pipeline will be compatible with
     * @param pipelineLayoutHandle The pipeline layout defining descriptor set layouts and push constants
     * @param vertexLayout The vertex attribute layout describing vertex buffer structure
     * @return A new VkPipeline instance ready for rendering
     */
    fun createGraphicsPipeline(
        logicalDevice: LogicalDevice,
        renderPass: RenderPass,
        pipelineLayoutHandle: VkPipelineLayout,
        vertexLayout: VertexLayout<*>,
    ): VkPipeline = MemoryStack.stackPush().use { stack ->
        val vertexShaderHandle = loadShader(logicalDevice, "/shaders/vert.spv")
        val fragmentShaderHandle = loadShader(logicalDevice, "/shaders/frag.spv")
        val shaderStages = createShaderStageCreateInfo(stack, vertexShaderHandle, fragmentShaderHandle)
        val pipelineCreateInfos =
            createPipelineCreateInfoBuffer(stack, shaderStages, pipelineLayoutHandle, renderPass, vertexLayout)

        val pPipelines = stack.mallocLong(1)
        vkCreateGraphicsPipelines(
            logicalDevice.handle,
            VK_NULL_HANDLE,
            pipelineCreateInfos,
            null,
            pPipelines,
        ).validateVulkanSuccess("Create graphics pipeline", "Failed to create graphics pipeline")

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
        vertexLayout: VertexLayout<*>,
    ): VkGraphicsPipelineCreateInfo.Buffer {
        val vertexInputInfo = setupVertexShaderInputInfo(stack, vertexLayout)
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
     * Loads and creates a shader module from the specified resource path.
     *
     * Reads the compiled SPIR-V shader code from resources and creates
     * a Vulkan shader module. The shader code must be pre-compiled to SPIR-V
     * format (typically using glslc compiler).
     *
     * @param logicalDevice The logical device to create the shader module on
     * @param path Resource path to the compiled SPIR-V shader file
     * @return Handle to the created shader module
     * @throws IllegalStateException if shader file cannot be found or shader creation fails
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

    /**
     * Creates shader stage configuration for vertex and fragment shaders.
     *
     * Configures the pipeline stages for both vertex and fragment shaders,
     * setting up their entry points and shader module references.
     *
     * @param stack Memory stack for allocating Vulkan structures
     * @param vertexShaderHandle Handle to the compiled vertex shader module
     * @param fragmentShaderHandle Handle to the compiled fragment shader module
     * @return Buffer containing shader stage configurations for both shaders
     */
    private fun createShaderStageCreateInfo(
        stack: MemoryStack,
        vertexShaderHandle: Handle,
        fragmentShaderHandle: Handle,
    ): VkPipelineShaderStageCreateInfo.Buffer {
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
        return shaderStages
    }
}
