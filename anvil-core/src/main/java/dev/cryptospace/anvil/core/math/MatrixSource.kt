package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

/**
 * Anything that can write a 4x4 matrix for the GPU without allocating, e.g. a [Mat4] or a cached transform.
 */
interface MatrixSource {

    /**
     * Writes the matrix in column-major order at the absolute [offset], leaving the buffer's position unchanged.
     */
    fun putColumnMajor(buffer: ByteBuffer, offset: Int)
}
