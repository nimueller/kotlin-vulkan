package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.gpu.GpuStruct
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.DescriptorSetLayout
import java.util.EnumSet

/**
 * Builder class for creating Vulkan pipeline layouts.
 * Pipeline layouts define the interface between shader stages and shader resources.
 *
 * @property logicalDevice The logical device used to create the pipeline layout
 */
class PipelineLayoutBuilder(
    val logicalDevice: LogicalDevice,
) {

    /** List of push constant ranges used in the pipeline layout */
    var pushConstantRanges = mutableListOf<PushConstantRange>()

    /** List of descriptor set layouts used in the pipeline layout */
    var descriptorSetLayouts = mutableListOf<DescriptorSetLayout>()

    /**
     * Adds a push constant range to the pipeline layout.
     *
     * @param stages Set of shader stages that will access this push constant range
     * @param struct Layout of the push constant data, including its offset
     */
    fun pushConstant(stages: EnumSet<ShaderType>, struct: GpuStruct) {
        pushConstantRanges.add(PushConstantRange(stages, struct.startOffset, struct.size))
    }

    /**
     * Creates a VkPipelineLayout object using the configured settings.
     *
     * @return The created pipeline layout
     */
    fun build(): VkPipelineLayout = PipelineLayoutFactory.createPipelineLayout(this)

    /**
     * Represents a push constant range in the pipeline layout.
     *
     * @property stages Set of shader stages that will access this push constant range
     * @property offset Offset in bytes from the start of the push constant block
     * @property size Size in bytes of the push constant range
     */
    data class PushConstantRange(
        val stages: EnumSet<ShaderType>,
        val offset: Int,
        val size: Int,
    )
}
