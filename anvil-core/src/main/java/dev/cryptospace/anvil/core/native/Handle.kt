package dev.cryptospace.anvil.core.native

private const val HEXADECIMAL = 16

fun Long.asHexString(): String = "0x${toString(HEXADECIMAL)}"

@JvmInline
value class Handle(
    val value: Long,
) {

    override fun toString(): String = value.asHexString()
}
