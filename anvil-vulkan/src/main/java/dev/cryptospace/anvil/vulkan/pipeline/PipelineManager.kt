package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.DescriptorSetManager
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderManager
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout
import java.util.EnumSet

/**
 * Creates and caches graphics pipelines. Pipelines are shared by all materials with the same [PipelineKey].
 */
class PipelineManager(
    private val logicalDevice: LogicalDevice,
    private val renderPass: RenderPass,
    descriptorSetManager: DescriptorSetManager,
    private val shaderManager: ShaderManager,
) : NativeResource() {

    private val pipelines = mutableMapOf<PipelineKey, Pipeline>()

    val pipelineCount: Int
        get() = pipelines.size

    private fun requireShader(path: String): ByteArray =
        PipelineManager::class.java.getResourceAsStream(path)?.use { it.readAllBytes() }
            ?: error("Shader $path not found at classpath")

    private fun loadShader(path: String, type: ShaderType) = shaderManager.uploadShader(requireShader(path), type)

    private val defaultShaders2D = PipelineKey.ShaderPair(
        vertex = loadShader("/shaders/shader2D.vert.spv", ShaderType.VERTEX),
        fragment = loadShader("/shaders/shader2D.frag.spv", ShaderType.FRAGMENT),
    )

    private val defaultShaders3D = PipelineKey.ShaderPair(
        vertex = loadShader("/shaders/shader.vert.spv", ShaderType.VERTEX),
        fragment = loadShader("/shaders/shader.frag.spv", ShaderType.FRAGMENT),
    )

    /**
     * The layout shared by all pipelines: per-frame data in set 0, the bindless texture array in set 1,
     * and the [PushConstants] for per-draw data.
     */
    val pipelineLayout =
        PipelineLayoutBuilder(logicalDevice = logicalDevice).apply {
            pushConstant(EnumSet.of(ShaderType.VERTEX), PushConstants.VERTEX_OFFSET, PushConstants.VERTEX_SIZE)
            pushConstant(EnumSet.of(ShaderType.FRAGMENT), PushConstants.FRAGMENT_OFFSET, PushConstants.FRAGMENT_SIZE)
            descriptorSetLayouts.add(descriptorSetManager.frameDescriptorSet.descriptorSetLayout)
            descriptorSetLayouts.add(descriptorSetManager.textureDescriptorSet.descriptorSetLayout)
        }.build()

    fun getPipeline(vertexLayout: VertexLayout<*>, material: Material?): Pipeline =
        getPipeline(PipelineKey.of(vertexLayout, material, ::defaultShadersFor))

    fun getPipeline(key: PipelineKey): Pipeline = pipelines.getOrPut(key) { createPipeline(key) }

    private fun defaultShadersFor(vertexLayout: VertexLayout<*>): PipelineKey.ShaderPair? = when (vertexLayout) {
        TexturedVertex2 -> defaultShaders2D
        TexturedVertex3 -> defaultShaders3D
        else -> null
    }

    private fun createPipeline(key: PipelineKey): Pipeline {
        logger.debug("Creating pipeline for {}", key)

        return PipelineBuilder(
            logicalDevice = logicalDevice,
            renderPass = renderPass,
            pipelineLayout = pipelineLayout,
            shaderManager = shaderManager,
        ).apply {
            vertexLayout(key.vertexLayout)
            shaderModule(ShaderType.VERTEX, key.vertexShader)
            shaderModule(ShaderType.FRAGMENT, key.fragmentShader)
            blendMode = key.blendMode
            cullMode = key.cullMode
            depthTest = key.depthTest
            depthWrite = key.depthWrite
        }.build()
    }

    override fun destroy() {
        pipelines.values.forEach(Pipeline::close)
        pipelines.clear()
        vkDestroyPipelineLayout(logicalDevice.handle, pipelineLayout.value, null)
    }

    companion object {

        @JvmStatic
        private val logger = logger<PipelineManager>()
    }
}
