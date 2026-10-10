package dev.cryptospace.anvil.core.scene

/**
 * Which polygon faces are discarded during rasterization.
 * Front faces are counter-clockwise.
 */
enum class CullMode {
    NONE,
    FRONT,
    BACK,
}
