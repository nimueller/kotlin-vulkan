package dev.cryptospace.anvil.core.native

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeType
import java.nio.ByteBuffer

data class UniformBufferObject(
    val view: Mat4,
    val projection: Mat4,
) : NativeType {

    override val byteSize: Int
        get() = BYTE_SIZE

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        view.toByteBuffer(byteBuffer)
        projection.toByteBuffer(byteBuffer)
    }

    companion object {

        const val BYTE_SIZE = 2 * Mat4.BYTE_SIZE

        val identity = UniformBufferObject(
            view = Mat4.identity,
            projection = Mat4.identity,
        )
    }
}
