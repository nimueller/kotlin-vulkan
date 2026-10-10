package dev.cryptospace.anvil.vulkan.pipeline.shader

import dev.cryptospace.anvil.core.gpu.GpuType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal SPIR-V reader that extracts the layout of push constant and uniform blocks,
 * enough to verify that a shader agrees with the engine's [dev.cryptospace.anvil.core.gpu.GpuStruct] declarations.
 */
class SpirvReflection(
    bytes: ByteArray,
) {

    /**
     * A struct member as declared by the shader.
     *
     * @property name Member name, or null if debug names were stripped
     * @property type Member type, or for runtime arrays the element type; null if the engine does not support it
     * @property arrayStride Byte distance between elements if the member is a runtime array (`T name[]`), else null
     */
    data class Member(
        val name: String?,
        val offset: Int,
        val type: GpuType?,
        val arrayStride: Int? = null,
    )

    data class Block(
        val name: String?,
        val members: List<Member>,
    )

    private val names = mutableMapOf<Int, String>()
    private val memberNames = mutableMapOf<Pair<Int, Int>, String>()
    private val memberOffsets = mutableMapOf<Pair<Int, Int>, Int>()
    private val structMemberTypes = mutableMapOf<Int, IntArray>()
    private val pointerTargets = mutableMapOf<Int, Int>()
    private val types = mutableMapOf<Int, GpuType?>()
    private val scalarTypes = mutableMapOf<Int, ScalarType>()
    private val runtimeArrayElementTypes = mutableMapOf<Int, Int>()
    private val arrayStrides = mutableMapOf<Int, Int>()
    private val descriptorSets = mutableMapOf<Int, Int>()
    private val bindings = mutableMapOf<Int, Int>()

    private class Variable(
        val id: Int,
        val storageClass: Int,
        val pointerTypeId: Int,
    )

    private val variables = mutableListOf<Variable>()

    private enum class ScalarType { FLOAT32, INT32, OTHER }

    init {
        require(bytes.size >= HEADER_WORDS * Int.SIZE_BYTES && bytes.size % Int.SIZE_BYTES == 0) {
            "Shader code is not a SPIR-V module"
        }
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        require(words.get(0) == MAGIC_NUMBER) { "Shader code is not a little-endian SPIR-V module" }

        var position = HEADER_WORDS
        while (position < words.limit()) {
            val instruction = words.get(position)
            val wordCount = instruction ushr 16
            require(wordCount > 0 && position + wordCount <= words.limit()) {
                "Malformed SPIR-V instruction at word $position"
            }
            val operands = IntArray(wordCount - 1) { index -> words.get(position + 1 + index) }
            parseInstruction(instruction and 0xFFFF, operands)
            position += wordCount
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun parseInstruction(opcode: Int, operands: IntArray) {
        when (opcode) {
            OP_NAME -> names[operands[0]] = decodeString(operands, 1)

            OP_MEMBER_NAME -> memberNames[operands[0] to operands[1]] = decodeString(operands, 2)

            OP_TYPE_INT -> scalarTypes[operands[0]] = if (operands[1] == 32) ScalarType.INT32 else ScalarType.OTHER

            OP_TYPE_FLOAT -> scalarTypes[operands[0]] = if (operands[1] == 32) ScalarType.FLOAT32 else ScalarType.OTHER

            OP_TYPE_VECTOR -> types[operands[0]] = vectorType(scalarTypes[operands[1]], operands[2])

            OP_TYPE_MATRIX -> types[operands[0]] = matrixType(types[operands[1]], operands[2])

            OP_TYPE_RUNTIME_ARRAY -> runtimeArrayElementTypes[operands[0]] = operands[1]

            OP_TYPE_STRUCT -> structMemberTypes[operands[0]] = operands.copyOfRange(1, operands.size)

            OP_TYPE_POINTER -> pointerTargets[operands[0]] = operands[2]

            OP_VARIABLE ->
                variables +=
                    Variable(id = operands[1], storageClass = operands[2], pointerTypeId = operands[0])

            OP_DECORATE -> when (operands[1]) {
                DECORATION_DESCRIPTOR_SET -> descriptorSets[operands[0]] = operands[2]
                DECORATION_BINDING -> bindings[operands[0]] = operands[2]
                DECORATION_ARRAY_STRIDE -> arrayStrides[operands[0]] = operands[2]
            }

            OP_MEMBER_DECORATE -> if (operands[2] == DECORATION_OFFSET) {
                memberOffsets[operands[0] to operands[1]] = operands[3]
            }
        }
    }

    private fun vectorType(component: ScalarType?, count: Int): GpuType? = when {
        component == ScalarType.FLOAT32 && count == 2 -> GpuType.VEC2
        component == ScalarType.FLOAT32 && count == 3 -> GpuType.VEC3
        component == ScalarType.FLOAT32 && count == 4 -> GpuType.VEC4
        else -> null
    }

    private fun matrixType(column: GpuType?, columns: Int): GpuType? =
        if (column == GpuType.VEC4 && columns == 4) GpuType.MAT4 else null

    private fun typeOf(typeId: Int): GpuType? = types[typeId] ?: when (scalarTypes[typeId]) {
        ScalarType.FLOAT32 -> GpuType.FLOAT
        ScalarType.INT32 -> GpuType.INT
        else -> null
    }

    private fun blockOf(variable: Variable): Block? {
        val structId = pointerTargets[variable.pointerTypeId] ?: return null
        val memberTypes = structMemberTypes[structId] ?: return null
        return Block(
            name = names[structId],
            members = memberTypes.mapIndexed { index, typeId ->
                val elementTypeId = runtimeArrayElementTypes[typeId]
                Member(
                    name = memberNames[structId to index],
                    offset = memberOffsets[structId to index] ?: error("Member $index of block lacks an offset"),
                    type = typeOf(elementTypeId ?: typeId),
                    arrayStride = elementTypeId?.let { arrayStrides[typeId] },
                )
            },
        )
    }

    /** The push constant block, or null if the shader declares none. */
    val pushConstantBlock: Block? by lazy {
        variables.firstOrNull { variable -> variable.storageClass == STORAGE_CLASS_PUSH_CONSTANT }?.let(::blockOf)
    }

    /** The uniform or storage buffer block bound at ([set], [binding]), or null if the shader declares none. */
    fun descriptorBlock(set: Int, binding: Int): Block? = variables
        .firstOrNull { variable ->
            // SPIR-V 1.0 declares storage buffers with the Uniform storage class, later versions use StorageBuffer
            (variable.storageClass == STORAGE_CLASS_UNIFORM || variable.storageClass == STORAGE_CLASS_STORAGE_BUFFER) &&
                descriptorSets[variable.id] == set &&
                bindings[variable.id] == binding
        }
        ?.let(::blockOf)

    private fun decodeString(operands: IntArray, startIndex: Int): String {
        val bytes = ByteBuffer.allocate((operands.size - startIndex) * Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        for (index in startIndex until operands.size) {
            bytes.putInt(operands[index])
        }
        val length = bytes.array().indexOf(0).let { terminator -> if (terminator < 0) bytes.capacity() else terminator }
        return String(bytes.array(), 0, length, Charsets.UTF_8)
    }

    private companion object {
        const val MAGIC_NUMBER = 0x07230203
        const val HEADER_WORDS = 5

        const val OP_NAME = 5
        const val OP_MEMBER_NAME = 6
        const val OP_TYPE_INT = 21
        const val OP_TYPE_FLOAT = 22
        const val OP_TYPE_VECTOR = 23
        const val OP_TYPE_MATRIX = 24
        const val OP_TYPE_RUNTIME_ARRAY = 29
        const val OP_TYPE_STRUCT = 30
        const val OP_TYPE_POINTER = 32
        const val OP_VARIABLE = 59
        const val OP_DECORATE = 71
        const val OP_MEMBER_DECORATE = 72

        const val DECORATION_ARRAY_STRIDE = 6
        const val DECORATION_BINDING = 33
        const val DECORATION_DESCRIPTOR_SET = 34
        const val DECORATION_OFFSET = 35

        const val STORAGE_CLASS_UNIFORM = 2
        const val STORAGE_CLASS_PUSH_CONSTANT = 9
        const val STORAGE_CLASS_STORAGE_BUFFER = 12
    }
}
