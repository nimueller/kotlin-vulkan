package dev.cryptospace.anvil.core

interface BitmaskEnum {

    val bitmask: Int

    companion object {

        fun Collection<BitmaskEnum>.toBitmask(): Int = map { it.bitmask }.reduceOrNull { acc, i -> acc or i } ?: 0
    }
}
