package dev.cryptospace.anvil.core.native

@JvmInline
value class Handle(
    val value: Long,
) {

    override fun toString(): String = "0x${value.toString(16)}"
}
