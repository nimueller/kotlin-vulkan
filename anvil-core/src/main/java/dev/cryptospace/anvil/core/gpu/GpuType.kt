package dev.cryptospace.anvil.core.gpu

/**
 * Scalar, vector and matrix types that can appear in GPU structs.
 *
 * @property size Size in bytes
 * @property baseAlignment Alignment under the std140 and std430 rules (identical for non-array members)
 * @property glslName Type name in GLSL, for error messages
 */
enum class GpuType(
    val size: Int,
    val baseAlignment: Int,
    val glslName: String,
) {
    FLOAT(4, 4, "float"),
    INT(4, 4, "int"),
    VEC2(8, 8, "vec2"),
    VEC3(12, 16, "vec3"),
    VEC4(16, 16, "vec4"),
    MAT4(64, 16, "mat4"),
}
