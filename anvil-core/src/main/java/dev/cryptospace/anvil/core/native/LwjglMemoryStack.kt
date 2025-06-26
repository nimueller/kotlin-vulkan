package dev.cryptospace.anvil.core.native

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import java.nio.IntBuffer

data class LwjglMemoryStack(
    override val memoryStack: MemoryStack,
) : AnvilMemoryStack {
    override fun push(): AnvilMemoryStack = LwjglMemoryStack(memoryStack.push())

    override fun mallocInts(count: Int): IntBuffer = memoryStack.mallocInt(count)

    override fun mallocPointers(count: Int): PointerBuffer = memoryStack.mallocPointer(count)

    override fun close() {
        memoryStack.close()
    }
}
