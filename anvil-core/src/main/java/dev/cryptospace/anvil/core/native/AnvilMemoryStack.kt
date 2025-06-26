package dev.cryptospace.anvil.core.native

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import java.nio.IntBuffer

interface AnvilMemoryStack : AutoCloseable {
    val memoryStack: MemoryStack

    fun push(): AnvilMemoryStack

    fun mallocInts(count: Int): IntBuffer

    fun mallocPointers(count: Int): PointerBuffer
}
