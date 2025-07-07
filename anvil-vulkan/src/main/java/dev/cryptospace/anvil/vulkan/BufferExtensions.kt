package dev.cryptospace.anvil.vulkan

import java.nio.LongBuffer

fun LongBuffer.forEach(receiver: (Long) -> Unit) {
    position(0)
    while (hasRemaining()) {
        receiver(get())
    }
    position(0)
}
