package dev.cryptospace.anvil.core.native

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeBuffer
import java.nio.ByteBuffer

data class UniformBufferObject(
    val model: Mat4,
    val view: Mat4,
    val projection: Mat4,
) : NativeBuffer {

    override val byteSize: Int = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        model.toByteBuffer(byteBuffer)
        view.toByteBuffer(byteBuffer)
        projection.toByteBuffer(byteBuffer)
    }

    companion object {

        const val BYTE_SIZE = 3 * Mat4.BYTE_SIZE
    }
}
