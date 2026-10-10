package dev.cryptospace.anvil.core.gpu

/**
 * Memory layout rules for GPU structs.
 */
enum class LayoutRule {

    /** Uniform buffers. */
    STD140,

    /** Push constants and storage buffers. */
    STD430,

    /** Tightly packed, as used for vertex attributes. */
    SCALAR,

    ;

    fun alignmentOf(type: GpuType): Int = when (this) {
        // std140 and std430 only differ for arrays and nested structs, which are not supported yet
        STD140, STD430 -> type.baseAlignment

        SCALAR -> Float.SIZE_BYTES
    }
}
