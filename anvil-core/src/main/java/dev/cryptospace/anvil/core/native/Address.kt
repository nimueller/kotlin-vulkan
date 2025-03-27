package dev.cryptospace.anvil.core.native

@JvmInline
value class Address(val handle: Long) {

    override fun toString(): String {
        return "0x${handle.toString(16)}"
    }
}
