package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
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
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines
import org.lwjgl.vulkan.VK10.vkCreatePipelineLayout
import org.lwjgl.vulkan.VK10.vkCreateShaderModule
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import org.lwjgl.vulkan.VK10.vkDestroyShaderModule
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

data class GraphicsPipeline(
    val logicalDevice: LogicalDevice,
    val renderPass: RenderPass,
) : NativeResource() {

    private val pipelineLayout = MemoryUtil.memAllocLong(1)

    val handle: Handle = MemoryStack.stackPush().use { stack ->
        val vertexShader = GraphicsPipeline::class.java.getResourceAsStream("/shaders/vert.spv")?.readAllBytes()
            ?: throw IllegalStateException("Vertex shader not found")
        val fragmentShader = GraphicsPipeline::class.java.getResourceAsStream("/shaders/frag.spv")?.readAllBytes()
            ?: throw IllegalStateException("Fragment shader not found")

        val vertexShaderModuleHandle = createShaderModule(vertexShader)
        val fragmentShaderModuleHandle = createShaderModule(fragmentShader)

        val vertexShaderCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage(VK_SHADER_STAGE_VERTEX_BIT)
            module(vertexShaderModuleHandle.value)
            pName(stack.UTF8("main"))
        }
        val fragmentShaderCreateInfo = VkPipelineShaderStageCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            module(fragmentShaderModuleHandle.value)
            pName(stack.UTF8("main"))
        }

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
            .put(vertexShaderCreateInfo)
            .put(fragmentShaderCreateInfo)
            .flip()

        val vertexInputInfo = setupVertexShaderInputInfo(stack)
        val dynamicState = setupDynamicState(stack)
        val inputAssembly = setupVertexInputAssembly(stack)
        val viewportState = setupViewport(stack)
        val rasterizer = setupRasterizer(stack)
        val multisampling = setupMultisampling(stack)
        val colorBlending = setupColorBlending(stack)

        val pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            pSetLayouts(null)
            pPushConstantRanges(null)
        }

        vkCreatePipelineLayout(
            logicalDevice.handle,
            pipelineLayoutCreateInfo,
            null,
            pipelineLayout,
        ).validateVulkanSuccess()

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
            layout(pipelineLayout[0])
            renderPass(renderPass.handle.value)
            subpass(0)
            basePipelineHandle(VK_NULL_HANDLE)
            basePipelineIndex(-1)
        }
        val pipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            .put(pipelineCreateInfo)
            .flip()

        val handle = stack.mallocLong(1)
        vkCreateGraphicsPipelines(logicalDevice.handle, VK_NULL_HANDLE, pipelineCreateInfos, null, handle)
            .validateVulkanSuccess()

        vkDestroyShaderModule(logicalDevice.handle, vertexShaderModuleHandle.value, null)
        vkDestroyShaderModule(logicalDevice.handle, fragmentShaderModuleHandle.value, null)

        Handle(handle[0])
    }

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

    private fun setupVertexShaderInputInfo(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo =
        VkPipelineVertexInputStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            pVertexBindingDescriptions(null)
            pVertexAttributeDescriptions(null)
        }

    private fun setupDynamicState(stack: MemoryStack): VkPipelineDynamicStateCreateInfo =
        VkPipelineDynamicStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))
        }

    private fun setupVertexInputAssembly(stack: MemoryStack): VkPipelineInputAssemblyStateCreateInfo =
        VkPipelineInputAssemblyStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            primitiveRestartEnable(false)
        }

    private fun setupViewport(stack: MemoryStack): VkPipelineViewportStateCreateInfo =
        VkPipelineViewportStateCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            viewportCount(1)
            scissorCount(1)
        }

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

    private fun createShaderModule(shaderCode: ByteArray): Handle = MemoryStack.stackPush().use { stack ->
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

    override fun destroy() {
        vkDestroyPipeline(logicalDevice.handle, handle.value, null)
        vkDestroyPipelineLayout(logicalDevice.handle, pipelineLayout[0], null)
    }
}
