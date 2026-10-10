package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.gpu.GpuStruct
import dev.cryptospace.anvil.core.gpu.LayoutRule
import dev.cryptospace.anvil.core.scene.MaterialProperties
import java.nio.ByteBuffer

/*
 * The data the engine passes to shaders. These declarations are the single source of truth: every shader is
 * checked against them when it is uploaded (see ShaderInterfaceValidator), so a shader whose blocks disagree
 * with the CPU side fails immediately instead of rendering garbage.
 *
 * Shaders declare them as:
 *
 *   // vertex stage
 *   layout (set = 0, binding = 0) uniform Camera { mat4 view; mat4 projection; } camera;
 *   layout (set = 0, binding = 1) readonly buffer Instances { mat4 models[]; } instances;   // index: gl_InstanceIndex
 *
 *   // fragment stage
 *   layout (push_constant) uniform PushConstants {
 *       vec4 albedo; float roughness; float metallic; float emissive; float ao; int textureIndex;
 *   } pushConstants;
 */

/**
 * Per-frame camera data in descriptor set 0, binding 0.
 */
object CameraUniform : GpuStruct(LayoutRule.STD140) {
    val view by mat4()
    val projection by mat4()
}

/**
 * One element of the per-frame instance storage buffer in descriptor set 0, binding 1.
 * Instanced draws read their element with `gl_InstanceIndex`.
 */
object InstanceData : GpuStruct(LayoutRule.STD430) {
    val model by mat4()
}

/**
 * Per-material push constants of the fragment stage. They stay valid across draws and pipeline switches,
 * so they only need to be pushed when the material changes.
 */
object MaterialPushConstants : GpuStruct(LayoutRule.STD430) {
    val albedo by vec4()
    val roughness by float()
    val metallic by float()
    val emissive by float()
    val ao by float()
    val textureIndex by int()

    /**
     * Texture index telling the shader to use the vertex color instead of sampling a texture.
     */
    const val NO_TEXTURE_INDEX = 0

    fun write(buffer: ByteBuffer, properties: MaterialProperties, textureIndex: Int) {
        albedo.put(buffer, properties.albedo)
        roughness.put(buffer, properties.roughness)
        metallic.put(buffer, properties.metallic)
        emissive.put(buffer, properties.emissive)
        ao.put(buffer, properties.ao)
        this.textureIndex.put(buffer, textureIndex)
    }
}
