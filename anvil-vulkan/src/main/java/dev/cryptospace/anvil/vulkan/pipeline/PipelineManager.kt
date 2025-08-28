package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeTypeLayout
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.mesh.Registry
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.DescriptorSetManager
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderManager
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderModule
import java.util.EnumSet

class PipelineManager(
    private val logicalDevice: LogicalDevice,
    private val renderPass: RenderPass,
    private val descriptorSetManager: DescriptorSetManager,
    private val shaderManager: ShaderManager,
) : NativeResource() {

    private val pipelineRegistry: Registry<Pipeline> = Registry()

    private val defaultVertexShader =
        shaderManager.uploadShader(
            shaderCode = ShaderModule::class.java.getResourceAsStream("/shaders/vert.spv")?.readAllBytes()
                ?: error("Shader not found at classpath"),
            shaderType = ShaderType.VERTEX,
        )

    private val defaultFragmentShader =
        shaderManager.uploadShader(
            shaderCode = ShaderModule::class.java.getResourceAsStream("/shaders/frag.spv")?.readAllBytes()
                ?: error("Shader not found at classpath"),
            shaderType = ShaderType.FRAGMENT,
        )

    val pipelineTextured3DLayout =
        PipelineLayoutBuilder(logicalDevice = logicalDevice).apply {
            pushConstant(EnumSet.of(ShaderType.VERTEX), Mat4)
            pushConstant(EnumSet.of(ShaderType.FRAGMENT), NativeTypeLayout.FLOAT)
            descriptorSetLayouts.add(descriptorSetManager.frameDescriptorSet.descriptorSetLayout)
            descriptorSetLayouts.add(descriptorSetManager.textureDescriptorSet.descriptorSetLayout)
        }.build()

    val pipelineTextured3D: Pipeline =
        PipelineBuilder(
            logicalDevice = logicalDevice,
            renderPass = renderPass,
            pipelineLayout = pipelineTextured3DLayout,
            shaderManager = shaderManager,
        ).apply {
            vertexLayout = TexturedVertex3
            shaderModule(ShaderType.VERTEX, defaultVertexShader)
            shaderModule(ShaderType.FRAGMENT, defaultFragmentShader)
        }.build()

    override fun destroy() {
        pipelineTextured3D.close()
    }
}
