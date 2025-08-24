package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.math.NativeTypeLayout
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.descriptor.DescriptorSetLayout
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
     * @param nativeTypeLayout Layout information for the push constant data
     */
    fun pushConstant(stages: EnumSet<ShaderStage>, nativeTypeLayout: NativeTypeLayout) {
        val offset = pushConstantRanges.sumOf { pushConstantRange -> pushConstantRange.size }
        pushConstantRanges.add(PushConstantRange(stages, offset, nativeTypeLayout.byteSize))
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
        val stages: EnumSet<ShaderStage>,
        val offset: Int,
        val size: Int,
    )
}
