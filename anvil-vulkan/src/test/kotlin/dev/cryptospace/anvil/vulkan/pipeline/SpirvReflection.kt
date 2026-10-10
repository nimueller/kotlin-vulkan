package dev.cryptospace.anvil.vulkan.pipeline

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal SPIR-V reader that extracts struct member offsets, enough to verify push constant layouts in tests.
 */
class SpirvReflection(
    bytes: ByteArray,
) {

    private val names = mutableMapOf<Int, String>()
    private val memberNames = mutableMapOf<Pair<Int, Int>, String>()
    private val memberOffsets = mutableMapOf<Pair<Int, Int>, Int>()

    init {
        val words = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        require(words.get(0) == MAGIC_NUMBER) { "Not a little-endian SPIR-V module" }

        var position = HEADER_WORDS
        while (position < words.limit()) {
            val instruction = words.get(position)
            val wordCount = instruction ushr 16
            val opcode = instruction and 0xFFFF
            require(wordCount > 0) { "Malformed SPIR-V instruction at word $position" }

            val operands = IntArray(wordCount - 1) { index -> words.get(position + 1 + index) }
            when (opcode) {
                OP_NAME -> names[operands[0]] = decodeString(operands, 1)

                OP_MEMBER_NAME -> memberNames[operands[0] to operands[1]] = decodeString(operands, 2)

                OP_MEMBER_DECORATE -> if (operands[2] == DECORATION_OFFSET) {
                    memberOffsets[operands[0] to operands[1]] = operands[3]
                }
            }
            position += wordCount
        }
    }

    /**
     * Returns the byte offset of every member of the struct named [structName].
     */
    fun memberOffsets(structName: String): Map<String, Int> {
        val structId = names.entries.single { (_, name) -> name == structName }.key
        return memberOffsets
            .filterKeys { (id, _) -> id == structId }
            .mapKeys { (key, _) -> memberNames.getValue(key) }
    }

    private fun decodeString(operands: IntArray, startIndex: Int): String {
        val bytes = ByteBuffer.allocate((operands.size - startIndex) * Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        operands.drop(startIndex).forEach(bytes::putInt)
        return String(bytes.array().takeWhile { byte -> byte != 0.toByte() }.toByteArray())
    }

    companion object {
        private const val MAGIC_NUMBER = 0x07230203
        private const val HEADER_WORDS = 5
        private const val OP_NAME = 5
        private const val OP_MEMBER_NAME = 6
        private const val OP_MEMBER_DECORATE = 72
        private const val DECORATION_OFFSET = 35

        fun fromResource(path: String): SpirvReflection = SpirvReflection(
            SpirvReflection::class.java.getResourceAsStream(path)?.use { it.readAllBytes() }
                ?: error("Shader $path not found on the classpath, was compileShaders run?"),
        )
    }
}
