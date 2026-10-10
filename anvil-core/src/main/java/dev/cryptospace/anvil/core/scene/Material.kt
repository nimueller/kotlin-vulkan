package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType

/**
 * Describes how a mesh is rendered.
 *
 * A material consists of two parts:
 * - **Pipeline state** ([shaders], [blendMode], [cullMode], [depthTest], [depthWrite]): materials sharing this state
 *   share a GPU pipeline, so changing it is comparatively expensive.
 * - **Parameters** ([texture], [properties]): cheap per-draw data that can differ between materials sharing a pipeline.
 *
 * Shaders that are not specified fall back to the backend's defaults for the mesh's vertex layout.
 *
 * A material is a plain description; it only takes effect once uploaded via the rendering system, which stores
 * a snapshot. Mutating the instance afterwards has no effect until it is passed to `updateMaterial`.
 */
data class Material(
    var texture: TextureId? = null,
    var shaders: MutableMap<ShaderType, ShaderId> = mutableMapOf(),
    var properties: MaterialProperties = MaterialProperties(),
    var blendMode: BlendMode = BlendMode.OPAQUE,
    var cullMode: CullMode = CullMode.BACK,
    var depthTest: Boolean = true,
    var depthWrite: Boolean = true,
) {

    /**
     * Creates an independent copy, so later mutations of either instance do not affect the other.
     */
    fun deepCopy(): Material = copy(
        shaders = shaders.toMutableMap(),
        properties = properties.copy(),
    )
}
