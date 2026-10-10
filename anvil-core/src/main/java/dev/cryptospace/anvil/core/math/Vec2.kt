package dev.cryptospace.anvil.core.math

data class Vec2(
    val x: Float,
    val y: Float,
) {

    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

    companion object {

        val zero = Vec2(0f, 0f)
    }
}
