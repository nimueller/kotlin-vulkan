package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeTypeLayout
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.TexturedVertex3
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
import java.util.EnumSet

class PipelineManager(
    private val logicalDevice: LogicalDevice,
    private val renderPass: RenderPass,
    private val descriptorSetManager: DescriptorSetManager,
    private val shaderManager: ShaderManager,
) : NativeResource() {

    private val pipelineRegistry: Registry<Pipeline> = Registry()
    private val pipelineIdRegistry: MutableMap<Pair<VertexLayout<*>, MaterialId?>, Int> = mutableMapOf()

    private fun requireShader(path: String): ByteArray =
        PipelineManager::class.java.getResourceAsStream(path)?.readAllBytes()
            ?: error("Shader $path not found at classpath")

    private val defaultVertexShader2D =
        shaderManager.uploadShader(
            shaderCode = requireShader("/shaders/shader2D.vert.spv"),
            shaderType = ShaderType.VERTEX,
        )

    private val defaultVertexShader3D =
        shaderManager.uploadShader(
            shaderCode = requireShader("/shaders/shader.vert.spv"),
            shaderType = ShaderType.VERTEX,
        )

    private val defaultFragmentShader2D =
        shaderManager.uploadShader(
            shaderCode = requireShader("/shaders/shader2D.frag.spv"),
            shaderType = ShaderType.FRAGMENT,
        )

    private val defaultFragmentShader3D =
        shaderManager.uploadShader(
            shaderCode = requireShader("/shaders/shader.frag.spv"),
            shaderType = ShaderType.FRAGMENT,
        )

    val basePipelineLayout =
        PipelineLayoutBuilder(logicalDevice = logicalDevice).apply {
            pushConstant(EnumSet.of(ShaderType.VERTEX), Mat4)
            pushConstant(EnumSet.of(ShaderType.FRAGMENT), NativeTypeLayout.FLOAT)
            descriptorSetLayouts.add(descriptorSetManager.frameDescriptorSet.descriptorSetLayout)
            descriptorSetLayouts.add(descriptorSetManager.textureDescriptorSet.descriptorSetLayout)
        }.build()

    fun getPipeline(vertexLayout: VertexLayout<*>, materialId: MaterialId?, material: VulkanMaterial?): Pipeline {
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
        materialId: MaterialId?,
        material: VulkanMaterial?,
    ): Pipeline {
        val pipeline = PipelineBuilder(
            logicalDevice = logicalDevice,
            renderPass = renderPass,
            pipelineLayout = basePipelineLayout,
            shaderManager = shaderManager,
        ).apply {
            vertexLayout(vertexLayout)

            setDefaultShaders(vertexLayout)

            material?.let { material ->
                for ((shaderType, shaderId) in material.shaders) {
                    shaderModule(shaderType, shaderId)
                }
            }
        }.build()
        val pipelineId = pipelineRegistry.add(pipeline)
        pipelineIdRegistry[vertexLayout to materialId] = pipelineId
        return pipeline
    }

    private fun PipelineBuilder.setDefaultShaders(vertexLayout: VertexLayout<*>) {
        if (vertexLayout == TexturedVertex2) {
            shaderModule(ShaderType.VERTEX, defaultVertexShader2D)
            shaderModule(ShaderType.FRAGMENT, defaultFragmentShader2D)
        } else if (vertexLayout == TexturedVertex3) {
            shaderModule(ShaderType.VERTEX, defaultVertexShader3D)
            shaderModule(ShaderType.FRAGMENT, defaultFragmentShader3D)
        }
    }

    override fun destroy() {
        pipelineRegistry.getAll().forEach { pipeline ->
            pipeline.close()
        }
    }
}
