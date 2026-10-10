package dev.cryptospace.anvil.core.scene

/**
 * How the output of a material is combined with what has already been drawn.
 */
enum class BlendMode {

    /** Output overwrites the framebuffer. Drawn before all blended materials. */
    OPAQUE,

    /** Classic transparency: `src * srcAlpha + dst * (1 - srcAlpha)`. Drawn after opaque materials. */
    ALPHA_BLEND,

    /** Output is added on top of the framebuffer, useful for glows and particles. */
    ADDITIVE,

    ;

    val isTransparent: Boolean
        get() = this != OPAQUE
}
