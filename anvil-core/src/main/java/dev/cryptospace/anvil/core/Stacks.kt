package dev.cryptospace.anvil.core

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack

fun MemoryStack.pushStrings(vararg strings: String): PointerBuffer {
    return pushStringList(strings.toList())
}

fun MemoryStack.pushStringList(list: List<String>): PointerBuffer {
    val pointer = mallocPointer(list.size)
    pointer.putAllStrings(this, list)
    pointer.flip()
    return pointer
}
