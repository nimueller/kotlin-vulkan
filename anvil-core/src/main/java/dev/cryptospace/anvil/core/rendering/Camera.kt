package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeType
import java.nio.ByteBuffer

data class Camera(
    var projectionMatrix: Mat4 = Mat4.identity,
    var viewMatrix: Mat4 = Mat4.identity,
) : NativeType {

    override val byteSize: Int
        get() = Mat4.BYTE_SIZE * 2

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        projectionMatrix.toByteBuffer(byteBuffer)
        viewMatrix.toByteBuffer(byteBuffer)
    }
}
