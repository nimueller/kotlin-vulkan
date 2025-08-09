package dev.cryptospace.anvil.core.math

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

interface NativeType {

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

@JvmName("toByteBufferCollection")
fun Collection<NativeType>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(sumOf { it.byteSize })
    forEach { it.toByteBuffer(buffer) }
    buffer.flip()
    return buffer
}

fun Collection<NativeType>.toByteBuffer(stack: MemoryStack): ByteBuffer {
    val buffer = stack.malloc(sumOf { it.byteSize })
    forEach { it.toByteBuffer(buffer) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferArray")
fun Array<out NativeType>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(sumOf { it.byteSize })
    forEach { it.toByteBuffer(buffer) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferShortCollection")
fun Collection<Short>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Short.SIZE_BYTES)
    forEach { buffer.putShort(it) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferUShortCollection")
fun Collection<UShort>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Short.SIZE_BYTES)
    forEach { buffer.putShort(it.toShort()) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferShortArray")
fun ShortArray.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Short.SIZE_BYTES)
    forEach { buffer.putShort(it) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferUShortArray")
fun Array<UShort>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Short.SIZE_BYTES)
    forEach { buffer.putShort(it.toShort()) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferIntArray")
fun IntArray.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Int.SIZE_BYTES)
    forEach { buffer.putInt(it) }
    buffer.flip()
    return buffer
}

@JvmName("toByteBufferUIntArray")
fun Array<UInt>.toByteBuffer(): ByteBuffer {
    val buffer = MemoryUtil.memAlloc(size * Int.SIZE_BYTES)
    forEach { buffer.putInt(it.toInt()) }
    buffer.flip()
    return buffer
}
