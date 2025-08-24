package dev.cryptospace.anvil.core.math

interface NativeTypeLayout {

    val byteSize: Int

    companion object {

        val FLOAT = object : NativeTypeLayout {
            override val byteSize: Int = Float.SIZE_BYTES
        }
    }
}
