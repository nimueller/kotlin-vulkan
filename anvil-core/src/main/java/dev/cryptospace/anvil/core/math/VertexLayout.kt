package dev.cryptospace.anvil.core.math

import java.nio.ByteBuffer

/**
 * Describes how vertices of type [V] are laid out in a vertex buffer.
 *
 * Attributes are declared in shader location order; their offsets and the vertex stride are derived from the
 * declarations, so there is a single place to change when a vertex format changes:
 *
 * ```kotlin
 * companion object : VertexLayout<MyVertex>() {
 *     val position = float3()   // location 0, offset 0
 *     val uv = float2()         // location 1, offset 12
 *
 *     override fun write(vertex: MyVertex, buffer: ByteBuffer, offset: Int) {
 *         position.put(buffer, offset, vertex.position)
 *         uv.put(buffer, offset, vertex.uv)
 *     }
 * }
 * ```
 *
 * Writers use absolute positions, so vertices can be written straight into mapped GPU memory without allocating.
 */
abstract class VertexLayout<V : Vertex> {

    private val _attributes = mutableListOf<Attribute>()
    val attributes: List<Attribute>
        get() = _attributes

    /** Size of one vertex in bytes, i.e. the vertex buffer stride. */
    val byteSize: Int
        get() = _attributes.sumOf { attribute -> attribute.format.size }

    sealed class Attribute(
        val location: Int,
        val format: AttributeFormat,
        val offset: Int,
    )

    class Float2Attribute internal constructor(
        location: Int,
        offset: Int,
    ) : Attribute(location, AttributeFormat.SIGNED_FLOAT_2D, offset) {

        fun put(buffer: ByteBuffer, vertexOffset: Int, x: Float, y: Float) {
            buffer.putFloat(vertexOffset + offset, x)
            buffer.putFloat(vertexOffset + offset + 4, y)
        }

        fun put(buffer: ByteBuffer, vertexOffset: Int, value: Vec2) = put(buffer, vertexOffset, value.x, value.y)
    }

    class Float3Attribute internal constructor(
        location: Int,
        offset: Int,
    ) : Attribute(location, AttributeFormat.SIGNED_FLOAT_3D, offset) {

        fun put(buffer: ByteBuffer, vertexOffset: Int, x: Float, y: Float, z: Float) {
            buffer.putFloat(vertexOffset + offset, x)
            buffer.putFloat(vertexOffset + offset + 4, y)
            buffer.putFloat(vertexOffset + offset + 8, z)
        }

        fun put(buffer: ByteBuffer, vertexOffset: Int, value: Vec3) =
            put(buffer, vertexOffset, value.x, value.y, value.z)
    }

    protected fun float2(): Float2Attribute = register(::Float2Attribute)

    protected fun float3(): Float3Attribute = register(::Float3Attribute)

    private fun <A : Attribute> register(create: (location: Int, offset: Int) -> A): A =
        create(_attributes.size, byteSize).also { attribute -> _attributes += attribute }

    /**
     * Writes one vertex at the absolute byte [offset] of [buffer].
     */
    abstract fun write(vertex: V, buffer: ByteBuffer, offset: Int)

    /**
     * Writes all [vertices] consecutively, starting at the beginning of [buffer].
     */
    fun write(vertices: Array<out V>, buffer: ByteBuffer) {
        val stride = byteSize
        for (index in vertices.indices) {
            write(vertices[index], buffer, index * stride)
        }
    }

    fun getAttributeDescriptions(binding: Int): List<AttributeDescription> = attributes.map { attribute ->
        AttributeDescription(binding, attribute.location, attribute.format, attribute.offset)
    }
}
