package dev.cryptospace.anvil.core

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack

fun PointerBuffer.toPointerList(): List<Long> {
    val list = mutableListOf<Long>()
    this.position(0)
    while (this.hasRemaining()) {
        list.add(this.get())
    }
    return list
}

fun PointerBuffer.toStringList(): List<String> {
    val list = mutableListOf<String>()
    this.position(0)
    while (this.hasRemaining()) {
        list.add(this.stringUTF8)
    }
    return list
}

/**
 * Pushes a list of pointers to this buffer.
 */
fun PointerBuffer.putAll(list: List<Long>) {
    list.forEach { this.put(it) }
}

/**
 * Pushes a list of strings to this buffer.
 */
fun PointerBuffer.putAllStrings(stack: MemoryStack, list: List<String>) {
    list.forEach { string ->
        val pointer = stack.UTF8(string)
        this.put(pointer)
    }
}
