package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderInterfaceValidator
import dev.cryptospace.anvil.vulkan.pipeline.shader.SpirvReflection
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Verifies the engine's shader interface declarations against real SPIR-V: the bundled shaders must pass, and
 * shaders that disagree with the CPU side must be rejected with a useful message.
 */
class ShaderInterfaceTest {

    @TempDir
    lateinit var tempDir: File

    private fun bundledShader(name: String) = SpirvReflection(
        ShaderInterfaceTest::class.java.getResourceAsStream("/shaders/$name.spv")?.use { it.readAllBytes() }
            ?: error("Shader $name not found on the classpath, was compileShaders run?"),
    )

    private fun compile(source: String, stage: ShaderType): SpirvReflection {
        val extension = if (stage == ShaderType.VERTEX) "vert" else "frag"
        val input = tempDir.resolve("test.$extension").apply { writeText(source) }
        val output = tempDir.resolve("test.$extension.spv")
        val process = ProcessBuilder("glslc", input.path, "-o", output.path).redirectErrorStream(true).start()
        val log = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "glslc failed:\n$log" }
        return SpirvReflection(output.readBytes())
    }

    @ParameterizedTest
    @CsvSource("shader.vert, VERTEX", "shader.frag, FRAGMENT", "shader2D.vert, VERTEX", "shader2D.frag, FRAGMENT")
    fun `bundled shaders match the engine's declarations`(name: String, stage: ShaderType) {
        assertEquals(emptyList<String>(), ShaderInterfaceValidator.validate(bundledShader(name), stage))
    }

    @Test
    fun `reflects the material push constant offsets`() {
        val block = checkNotNull(bundledShader("shader.frag").pushConstantBlock)

        assertEquals(
            MaterialPushConstants.fields.associate { field -> field.name to field.offset },
            block.members.associate { member -> checkNotNull(member.name) to member.offset },
        )
    }

    @Test
    fun `reflects the instance buffer as a mat4 runtime array`() {
        val member = checkNotNull(bundledShader("shader.vert").descriptorBlock(set = 0, binding = 1)).members.single()

        assertEquals("models", member.name)
        assertEquals(InstanceData.size, member.arrayStride)
        assertEquals(InstanceData.model.type, member.type)
    }

    @Test
    fun `rejects the original push constant layout bug`() {
        // textureIndex before albedo pushed albedo to offset 16 within the block, where the CPU never wrote it
        val shader = compile(
            """
            #version 450
            layout (push_constant) uniform PushConstants {
                int textureIndex;
                vec4 albedo;
            } pushConstants;
            layout (location = 0) out vec4 color;
            void main() { color = pushConstants.albedo * float(pushConstants.textureIndex); }
            """.trimIndent(),
            ShaderType.FRAGMENT,
        )

        val problems = ShaderInterfaceValidator.validate(shader, ShaderType.FRAGMENT)

        assertEquals(2, problems.size, problems.toString())
        assertTrue(problems.any { it.contains("albedo") && it.contains("@ 16") }, problems.toString())
    }

    @Test
    fun `rejects members the engine does not provide`() {
        val shader = compile(
            """
            #version 450
            layout (push_constant) uniform PushConstants { vec4 albedo; float shininess; } pushConstants;
            layout (location = 0) out vec4 color;
            void main() { color = pushConstants.albedo * pushConstants.shininess; }
            """.trimIndent(),
            ShaderType.FRAGMENT,
        )

        val problems = ShaderInterfaceValidator.validate(shader, ShaderType.FRAGMENT)

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("shininess"))
    }

    @Test
    fun `rejects instance buffers with the wrong element type`() {
        val shader = compile(
            """
            #version 450
            layout (set = 0, binding = 1) readonly buffer Instances { vec4 offsets[]; } instances;
            void main() { gl_Position = instances.offsets[gl_InstanceIndex]; }
            """.trimIndent(),
            ShaderType.VERTEX,
        )

        val problems = ShaderInterfaceValidator.validate(shader, ShaderType.VERTEX)

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("instance buffer"))
    }

    @Test
    fun `rejects vertex push constants, which the engine no longer provides`() {
        val shader = compile(
            """
            #version 450
            layout (push_constant) uniform PushConstants { mat4 model; } pushConstants;
            void main() { gl_Position = pushConstants.model * vec4(0.0); }
            """.trimIndent(),
            ShaderType.VERTEX,
        )

        val problems = ShaderInterfaceValidator.validate(shader, ShaderType.VERTEX)

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("instance buffer"))
    }

    @Test
    fun `rejects code that is not SPIR-V`() {
        assertFailsWith<IllegalArgumentException> { SpirvReflection(ByteArray(8)) }
        assertFailsWith<IllegalArgumentException> { SpirvReflection("#version 450".toByteArray()) }
    }
}
