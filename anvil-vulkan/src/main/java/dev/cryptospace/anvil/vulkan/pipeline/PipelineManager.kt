package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeTypeLayout
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.mesh.Registry
import dev.cryptospace.anvil.vulkan.mesh.VulkanMaterial
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
    private val pipelineIdRegistry: MutableMap<Pair<VertexLayout<*>, MaterialId>, Int> = mutableMapOf()

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

    val basePipelineLayout =
        PipelineLayoutBuilder(logicalDevice = logicalDevice).apply {
            pushConstant(EnumSet.of(ShaderType.VERTEX), Mat4)
            pushConstant(EnumSet.of(ShaderType.FRAGMENT), NativeTypeLayout.FLOAT)
            descriptorSetLayouts.add(descriptorSetManager.frameDescriptorSet.descriptorSetLayout)
            descriptorSetLayouts.add(descriptorSetManager.textureDescriptorSet.descriptorSetLayout)
        }.build()

    fun getPipeline(vertexLayout: VertexLayout<*>, materialId: MaterialId, material: VulkanMaterial): Pipeline {
        val pipelineId = pipelineIdRegistry[vertexLayout to materialId]

        if (pipelineId != null) {
            val pipeline = pipelineRegistry[pipelineId]

            if (pipeline != null) {
                return pipeline
            }
        }

        return createPipeline(vertexLayout, materialId, material)
    }

    private fun createPipeline(
        vertexLayout: VertexLayout<*>,
        materialId: MaterialId,
        material: VulkanMaterial,
    ): Pipeline {
        val pipeline = PipelineBuilder(
            logicalDevice = logicalDevice,
            renderPass = renderPass,
            pipelineLayout = basePipelineLayout,
            shaderManager = shaderManager,
        ).apply {
            vertexLayout(vertexLayout)

            shaderModule(ShaderType.VERTEX, defaultVertexShader)
            shaderModule(ShaderType.FRAGMENT, defaultFragmentShader)

            for ((shaderType, shaderId) in material.shaders) {
                shaderModule(shaderType, shaderId)
            }
        }.build()
        val pipelineId = pipelineRegistry.add(pipeline)
        pipelineIdRegistry[vertexLayout to materialId] = pipelineId
        return pipeline
    }

    override fun destroy() {
        pipelineRegistry.getAll().forEach { pipeline ->
            pipeline.close()
        }
    }
}
