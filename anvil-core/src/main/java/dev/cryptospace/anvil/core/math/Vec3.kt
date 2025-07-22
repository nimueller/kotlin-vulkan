package dev.cryptospace.anvil.core.math

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) {

    companion object {

        const val SIZE = 3 * Float.SIZE_BYTES
    }
}
