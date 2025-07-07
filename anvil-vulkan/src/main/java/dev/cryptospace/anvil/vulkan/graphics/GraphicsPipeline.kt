package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateShaderModule
import org.lwjgl.vulkan.VK10.vkDestroyShaderModule
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

class GraphicsPipeline(
    val logicalDevice: LogicalDevice,
) : NativeResource() {

    init {
        val vertexShader = GraphicsPipeline::class.java.getResourceAsStream("/shaders/vert.spv")?.readAllBytes()
            ?: throw IllegalStateException("Vertex shader not found")
        val fragmentShader = GraphicsPipeline::class.java.getResourceAsStream("/shaders/frag.spv")?.readAllBytes()
            ?: throw IllegalStateException("Fragment shader not found")

        val vertexShaderModuleHandle = createShaderModule(vertexShader)
        val fragmentShaderModuleHandle = createShaderModule(fragmentShader)

        MemoryStack.stackPush().use { stack ->
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

            val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2)
                .put(0, vertexShaderCreateInfo)
                .put(1, fragmentShaderCreateInfo)
                .flip()
        }

        vkDestroyShaderModule(logicalDevice.handle, vertexShaderModuleHandle.value, null)
        vkDestroyShaderModule(logicalDevice.handle, fragmentShaderModuleHandle.value, null)
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
        // Nothing to do at the moment
    }
}
