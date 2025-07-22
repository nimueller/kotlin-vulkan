package dev.cryptospace.anvil.core.math

data class Vec2(
    val x: Float,
    val y: Float,
) {

    companion object {

        const val SIZE = 2 * Float.SIZE_BYTES
    }
}
