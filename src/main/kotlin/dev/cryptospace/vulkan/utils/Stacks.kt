package dev.cryptospace.vulkan.utils

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack

fun MemoryStack.pushStrings(vararg strings: String): PointerBuffer {
    return pushStringList(strings.toList())
}

fun MemoryStack.pushStringList(list: List<String>): PointerBuffer {
    val pointer = mallocPointer(list.size)

    list.forEach { item ->
        pointer.put(ASCII(item))
    }

    pointer.flip()
    return pointer
}
