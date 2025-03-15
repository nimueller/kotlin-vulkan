package dev.cryptospace.vulkan.utils

import org.lwjgl.PointerBuffer

fun PointerBuffer.toList(): List<Long> {
    val list = mutableListOf<Long>()
    this.position(0)
    while (this.hasRemaining()) {
        list.add(this.get())
    }
    return list
}
