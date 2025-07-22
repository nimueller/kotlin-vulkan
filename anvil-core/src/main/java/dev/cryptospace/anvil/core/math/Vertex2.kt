package dev.cryptospace.anvil.core.math

data class Vertex2(
    val position: Vec2,
    val color: Vec3,
) {

    companion object {

        const val SIZE = Vec2.SIZE + Vec3.SIZE
    }
}
