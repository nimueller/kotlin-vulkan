package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.scene.MaterialProperties
import java.nio.ByteBuffer

/**
 * CPU-side mirror of the `PushConstants` blocks declared in the shaders.
 *
 * ```glsl
 * // vertex stage
 * layout (offset = 0)  mat4 model;
 * // fragment stage
 * layout (offset = 64) vec4 albedo;      // 64
 *                      float roughness;  // 80
 *                      float metallic;   // 84
 *                      float emissive;   // 88
 *                      float ao;         // 92
 *                      int textureIndex; // 96
 * ```
 *
 * Push constant blocks use the std430 layout, so a `vec4` must start at a multiple of 16 bytes.
 * Custom shaders must declare the same layout. `PushConstantsLayoutTest` verifies the bundled
 * shaders against these offsets.
 */
object PushConstants {

    const val VERTEX_OFFSET = 0
    const val VERTEX_SIZE = Mat4.BYTE_SIZE

    const val FRAGMENT_OFFSET = VERTEX_OFFSET + VERTEX_SIZE
    const val TEXTURE_INDEX_OFFSET = FRAGMENT_OFFSET + MaterialProperties.BYTE_SIZE
    const val FRAGMENT_SIZE = TEXTURE_INDEX_OFFSET + Int.SIZE_BYTES - FRAGMENT_OFFSET

    /**
     * Texture index telling the shader to use the vertex color instead of sampling a texture.
     */
    const val NO_TEXTURE_INDEX = 0

    /**
     * Writes the fragment stage block, which starts at [FRAGMENT_OFFSET].
     */
    fun writeFragment(buffer: ByteBuffer, properties: MaterialProperties, textureIndex: Int) {
        properties.toByteBuffer(buffer)
        buffer.putInt(textureIndex)
    }
}
