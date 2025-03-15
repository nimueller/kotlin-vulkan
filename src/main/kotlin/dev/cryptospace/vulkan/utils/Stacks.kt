package dev.cryptospace.vulkan.utils

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack

fun MemoryStack.pushStringList(list: List<String>): PointerBuffer {
    val pointer = mallocPointer(list.size)

    list.forEachIndexed { index, item ->
        pointer.put(index, ASCII(item))
    }

    pointer.flip()
    return pointer
}
