package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.VertexLayout
import dev.cryptospace.anvil.core.scene.BlendMode
import dev.cryptospace.anvil.core.scene.CullMode
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType

/**
 * Everything that requires a distinct Vulkan pipeline. Materials that only differ in parameters such as
 * textures or colors map to the same key and therefore share a pipeline.
 */
data class PipelineKey(
    val vertexLayout: VertexLayout<*>,
    val vertexShader: ShaderId,
    val fragmentShader: ShaderId,
    val blendMode: BlendMode,
    val cullMode: CullMode,
    val depthTest: Boolean,
    val depthWrite: Boolean,
) {

    data class ShaderPair(
        val vertex: ShaderId,
        val fragment: ShaderId,
    )

    companion object {

        private val DEFAULT_MATERIAL = Material()

        /**
         * Derives the pipeline key for drawing a mesh with [vertexLayout] using [material].
         * Shaders the material does not specify are taken from [defaultShaders].
         *
         * @param material The material, or null to use the default material
         * @param defaultShaders Default shaders for a vertex layout, or null if there are none
         * @throws IllegalArgumentException if a shader stage can neither be taken from the material nor the defaults
         */
        fun of(
            vertexLayout: VertexLayout<*>,
            material: Material?,
            defaultShaders: (VertexLayout<*>) -> ShaderPair?,
        ): PipelineKey {
            val effectiveMaterial = material ?: DEFAULT_MATERIAL
            val defaults by lazy { defaultShaders(vertexLayout) }

            fun shader(type: ShaderType, default: ShaderPair.() -> ShaderId): ShaderId = effectiveMaterial.shaders[type]
                ?: defaults?.default()
                ?: throw IllegalArgumentException(
                    "No default $type shader for vertex layout $vertexLayout, the material must specify one",
                )

            return PipelineKey(
                vertexLayout = vertexLayout,
                vertexShader = shader(ShaderType.VERTEX) { vertex },
                fragmentShader = shader(ShaderType.FRAGMENT) { fragment },
                blendMode = effectiveMaterial.blendMode,
                cullMode = effectiveMaterial.cullMode,
                depthTest = effectiveMaterial.depthTest,
                depthWrite = effectiveMaterial.depthWrite,
            )
        }
    }
}
