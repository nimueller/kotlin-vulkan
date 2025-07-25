package dev.cryptospace.anvil.core.math

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

interface NativeBuffer {

    val byteSize: Int

    fun toByteBuffer(byteBuffer: ByteBuffer)

    fun toByteBuffer(): ByteBuffer {
        val buffer = MemoryUtil.memAlloc(byteSize)
        toByteBuffer(buffer)
        buffer.flip()
        return buffer
    }

    fun toByteBuffer(stack: MemoryStack): ByteBuffer {
        val buffer = stack.malloc(byteSize)
        toByteBuffer(buffer)
        buffer.flip()
        return buffer
    }
}

fun Collection<NativeBuffer>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(sumOf { it.byteSize })
    forEach { it.toByteBuffer(buffer) }
    buffer.flip()
    return buffer
}

fun Collection<NativeBuffer>.toByteBuffer(stack: MemoryStack): ByteBuffer {
    val buffer = stack.malloc(sumOf { it.byteSize })
    forEach { it.toByteBuffer(buffer) }
    buffer.flip()
    return buffer
}
