package dev.cryptospace.anvil.core.gpu

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vec4
import java.nio.ByteBuffer
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Single source of truth for the memory layout of a struct shared between the CPU and shaders.
 *
 * Members are declared as delegated properties, in shader declaration order; their names must match the GLSL
 * member names, and offsets are derived from [rule]:
 *
 * ```kotlin
 * object CameraUniform : GpuStruct(LayoutRule.STD140) {
 *     val view by mat4()
 *     val projection by mat4()
 * }
 * CameraUniform.view.put(buffer, camera.view)
 * ```
 *
 * Writes use absolute positions and never change the buffer's position, so they allocate nothing. For arrays of
 * structs, pass the element's byte offset as `baseOffset`.
 *
 * @param rule Layout rule used by the shader block
 * @param startOffset Offset of the first byte of this struct inside its block, e.g. for push constant ranges that
 *   continue where another stage's range ends. Field offsets are absolute; writes are relative to [startOffset].
 */
abstract class GpuStruct(
    val rule: LayoutRule,
    val startOffset: Int = 0,
) {

    private val _fields = mutableListOf<Field>()
    val fields: List<Field>
        get() = _fields

    /** Absolute offset of the first byte after the last member. */
    val endOffset: Int
        get() = _fields.lastOrNull()?.let { field -> field.offset + field.type.size } ?: startOffset

    /** Number of bytes from [startOffset] to [endOffset], i.e. the size of a buffer holding this struct. */
    val size: Int
        get() = endOffset - startOffset

    sealed class Field(
        val name: String,
        val type: GpuType,
        /** Absolute offset, as seen by the shader. */
        val offset: Int,
        /** Offset relative to the start of a buffer holding this struct. */
        val bufferOffset: Int,
    ) {
        override fun toString(): String = "${type.glslName} $name @ $offset"
    }

    class FloatField internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.FLOAT, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Float) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Float) {
            buffer.putFloat(baseOffset + bufferOffset, value)
        }
    }

    class IntField internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.INT, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Int) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Int) {
            buffer.putInt(baseOffset + bufferOffset, value)
        }
    }

    class Vec2Field internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.VEC2, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Vec2) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Vec2) {
            buffer.putFloat(baseOffset + bufferOffset, value.x)
            buffer.putFloat(baseOffset + bufferOffset + 4, value.y)
        }
    }

    class Vec3Field internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.VEC3, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Vec3) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Vec3) {
            buffer.putFloat(baseOffset + bufferOffset, value.x)
            buffer.putFloat(baseOffset + bufferOffset + 4, value.y)
            buffer.putFloat(baseOffset + bufferOffset + 8, value.z)
        }
    }

    class Vec4Field internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.VEC4, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Vec4) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Vec4) {
            buffer.putFloat(baseOffset + bufferOffset, value.x)
            buffer.putFloat(baseOffset + bufferOffset + 4, value.y)
            buffer.putFloat(baseOffset + bufferOffset + 8, value.z)
            buffer.putFloat(baseOffset + bufferOffset + 12, value.w)
        }
    }

    class Mat4Field internal constructor(
        name: String,
        offset: Int,
        bufferOffset: Int,
    ) : Field(name, GpuType.MAT4, offset, bufferOffset) {
        fun put(buffer: ByteBuffer, value: Mat4) = put(buffer, 0, value)

        fun put(buffer: ByteBuffer, baseOffset: Int, value: Mat4) {
            value.putColumnMajor(buffer, baseOffset + bufferOffset)
        }
    }

    /**
     * Declares a member. Members are laid out in the order their properties are initialized,
     * which is declaration order.
     */
    protected class Member<F : Field>(
        private val type: GpuType,
        private val create: (name: String, offset: Int, bufferOffset: Int) -> F,
    ) {
        operator fun provideDelegate(struct: GpuStruct, property: KProperty<*>): ReadOnlyProperty<GpuStruct, F> {
            val offset = struct.alignUp(struct.endOffset, struct.rule.alignmentOf(type))
            val field = create(property.name, offset, offset - struct.startOffset)
            struct._fields += field
            return ReadOnlyProperty { _, _ -> field }
        }
    }

    protected fun float() = Member(GpuType.FLOAT, ::FloatField)

    protected fun int() = Member(GpuType.INT, ::IntField)

    protected fun vec2() = Member(GpuType.VEC2, ::Vec2Field)

    protected fun vec3() = Member(GpuType.VEC3, ::Vec3Field)

    protected fun vec4() = Member(GpuType.VEC4, ::Vec4Field)

    protected fun mat4() = Member(GpuType.MAT4, ::Mat4Field)

    private fun alignUp(offset: Int, alignment: Int): Int = (offset + alignment - 1) / alignment * alignment

    override fun toString(): String = "${this::class.simpleName}(${fields.joinToString()})"
}
