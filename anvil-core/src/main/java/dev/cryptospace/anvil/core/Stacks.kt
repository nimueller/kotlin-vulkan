package dev.cryptospace.anvil.core

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack

fun MemoryStack.pushStrings(vararg strings: String): PointerBuffer {
    return pushStringList(strings.toList())
}

fun MemoryStack.pushStringList(strings: Collection<String>): PointerBuffer {
    val pointer = mallocPointer(strings.size)
    pointer.putAllStrings(this, strings)
    pointer.flip()
    return pointer
}
