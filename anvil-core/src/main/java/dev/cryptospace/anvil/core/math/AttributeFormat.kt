package dev.cryptospace.anvil.core.math

enum class AttributeFormat(
    val size: Int,
) {
    SIGNED_FLOAT_2D(2 * Float.SIZE_BYTES),
    SIGNED_FLOAT_3D(3 * Float.SIZE_BYTES),
}
