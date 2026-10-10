package dev.cryptospace.anvil.vulkan.pipeline.shader

import dev.cryptospace.anvil.core.gpu.GpuStruct
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.pipeline.CameraUniform
import dev.cryptospace.anvil.vulkan.pipeline.InstanceData
import dev.cryptospace.anvil.vulkan.pipeline.MaterialPushConstants

/**
 * Checks that a shader's push constant and descriptor blocks match the engine's [GpuStruct] declarations.
 *
 * A shader may use a subset of the members, but every member it declares must exist on the CPU side with the same
 * offset and type. Members are matched by name, or by offset if the shader was compiled without debug names.
 */
object ShaderInterfaceValidator {

    private const val CAMERA_BINDING = 0
    private const val INSTANCES_BINDING = 1

    /**
     * @return Human-readable descriptions of all mismatches, empty if the shader is compatible
     */
    fun validate(reflection: SpirvReflection, stage: ShaderType): List<String> {
        val problems = mutableListOf<String>()

        when (stage) {
            ShaderType.VERTEX -> {
                reflection.pushConstantBlock?.let {
                    problems += "push constants: the engine provides none to the vertex stage, " +
                        "read the model matrix from the instance buffer (set 0, binding $INSTANCES_BINDING) instead"
                }
                reflection.descriptorBlock(set = 0, binding = CAMERA_BINDING)?.let { block ->
                    problems += compare("camera uniform (set 0, binding $CAMERA_BINDING)", block, CameraUniform)
                }
                reflection.descriptorBlock(set = 0, binding = INSTANCES_BINDING)?.let { block ->
                    problems += compareInstances(block)
                }
            }

            ShaderType.FRAGMENT -> reflection.pushConstantBlock?.let { block ->
                problems += compare("push constants", block, MaterialPushConstants)
            }
        }

        return problems
    }

    fun compare(description: String, block: SpirvReflection.Block, struct: GpuStruct): List<String> =
        block.members.mapNotNull { member ->
            val field = if (member.name != null) {
                struct.fields.firstOrNull { field -> field.name == member.name }
            } else {
                struct.fields.firstOrNull { field -> field.offset == member.offset }
            }

            when {
                field == null -> "$description: shader declares ${describe(member)}, which the engine does not provide"

                member.arrayStride != null || field.offset != member.offset || field.type != member.type ->
                    "$description: shader declares ${describe(member)}, but the engine writes $field"

                else -> null
            }
        }

    /**
     * The instance buffer is a runtime array whose elements are laid out like [InstanceData].
     * Only a single-member element, a plain `mat4` array, is supported so far.
     */
    private fun compareInstances(block: SpirvReflection.Block): List<String> {
        val description = "instance buffer (set 0, binding $INSTANCES_BINDING)"
        val element = InstanceData.fields.single()
        val member = block.members.singleOrNull()
            ?: return listOf("$description: expected a single runtime array, found ${block.members.size} members")

        val matches = member.offset == 0 &&
            member.arrayStride == InstanceData.size &&
            member.type == element.type
        return if (matches) {
            emptyList()
        } else {
            listOf(
                "$description: shader declares ${describe(member)} with stride ${member.arrayStride}, " +
                    "but the engine writes ${element.type.glslName}[] with stride ${InstanceData.size}",
            )
        }
    }

    private fun describe(member: SpirvReflection.Member): String =
        "${member.type?.glslName ?: "<unsupported type>"}${if (member.arrayStride != null) "[]" else ""} " +
            "${member.name ?: "<unnamed>"} @ ${member.offset}"
}
