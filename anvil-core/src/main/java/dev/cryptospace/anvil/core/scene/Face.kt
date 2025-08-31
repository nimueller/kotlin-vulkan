package dev.cryptospace.anvil.core.scene

data class Face(
    val indexA: UInt,
    val indexB: UInt,
    val indexC: UInt,
) {
    operator fun get(index: Int): UInt = when (index) {
        0 -> indexA
        1 -> indexB
        2 -> indexC
        else -> throw IllegalArgumentException("Invalid index: $index")
    }

    companion object {

        const val INDICES_SIZE = 3

        fun Array<Face>.toIndicesArray(): Array<UInt> = Array(size * INDICES_SIZE) { index ->
            val face = this[index / INDICES_SIZE]
            val index = face[index % INDICES_SIZE]
            index
        }
    }
}
