package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.math.Vec4

/**
 * Common properties for a material.
 * These properties are used for lighting and rendering calculations.
 */
data class MaterialProperties(
    /**
     * Base color (albedo) of the material.
     */
    var albedo: Vec4 = Vec4(1f, 1f, 1f, 1f),

    /**
     * Roughness of the material (0.0 = smooth, 1.0 = rough).
     */
    var roughness: Float = 0.5f,

    /**
     * Metallic property of the material (0.0 = non-metallic, 1.0 = metallic).
     */
    var metallic: Float = 0.0f,

    /**
     * How much light the material emits.
     */
    var emissive: Float = 0.0f,

    /**
     * Ambient occlusion factor.
     */
    var ao: Float = 1.0f,
)
