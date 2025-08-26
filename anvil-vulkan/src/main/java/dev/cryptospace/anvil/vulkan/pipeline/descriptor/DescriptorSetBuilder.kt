package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderStage
import org.lwjgl.vulkan.VK10
import java.util.EnumSet

/**
 * Builder class for creating descriptor sets and their layouts in Vulkan.
 * Provides a fluent interface for configuring descriptor set bindings and creating descriptor sets.
 */
class DescriptorSetBuilder {

    private var mutableBindings: MutableMap<Int, Binding> = mutableMapOf()

    /**
     * Stores the mapping of binding indices to their corresponding Binding configurations.
     * Each binding index represents a unique resource binding point in shaders,
     * and the associated Binding configuration defines the type, count, and shader stage
     * accessibility of that resource.
     */
    val bindings: MutableMap<Int, Binding>
        get() = mutableBindings

    /**
     * Controls whether the final binding in the descriptor set allows for a variable
     * number of descriptors. When set to true, the descriptor set layout will be created
     * with a VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT_EXT flag for the last binding.
     * This is useful for implementing array-like bindings where the size isn't known at
     * layout creation time.
     */
    var lastBindingHasVariableDescriptorCount: Boolean = false
        private set

    /**
     * Adds a new descriptor binding to the descriptor set layout.
     *
     * @param descriptorType The type of descriptor (uniform buffer, sampler, etc.)
     * @param descriptorCount Number of descriptors in this binding
     * @param stages Set of shader stages where this binding will be accessible
     * @throws IllegalStateException if attempting to add a binding after a variable descriptor count binding
     */
    fun binding(
        descriptorType: DescriptorType,
        descriptorCount: Int,
        stages: EnumSet<ShaderStage>,
    ): DescriptorSetBuilder {
        check(lastBindingHasVariableDescriptorCount.not()) {
            "Last binding already has variable descriptor count." +
                " More bindings are not allowed, check the Vulkan spec for details."
        }
        bindings[bindings.size] = Binding(descriptorType, descriptorCount, stages)
        return this
    }

    /**
     * Adds a new descriptor binding with variable descriptor count to the descriptor set layout.
     * This must be the last binding added to the descriptor set.
     *
     * @param descriptorType The type of descriptor (uniform buffer, sampler, etc.)
     * @param descriptorCount Maximum number of descriptors in this binding
     * @param stages Set of shader stages where this binding will be accessible
     */
    fun variableBinding(
        descriptorType: DescriptorType,
        descriptorCount: Int,
        stages: EnumSet<ShaderStage>,
    ): DescriptorSetBuilder {
        binding(descriptorType, descriptorCount, stages)
        lastBindingHasVariableDescriptorCount = true
        return this
    }

    /**
     * Creates a descriptor set layout and allocates descriptor sets based on the configured bindings.
     *
     * @param logicalDevice The logical device used to create the descriptor set layout and descriptor sets
     * @param descriptorPool The descriptor pool from which to allocate the descriptor sets
     * @param setCount Number of descriptor sets to allocate
     * @return A pair of the created descriptor set layout and descriptor set
     */
    fun build(logicalDevice: LogicalDevice, descriptorPool: DescriptorPool, setCount: Int = 1): Result {
        val descriptorSetLayout = DescriptorSetLayoutFactory.createDescriptorSetLayout(logicalDevice, this)
        val descriptorSet = DescriptorSetFactory.createDescriptorSet(
            logicalDevice = logicalDevice,
            descriptorSetBuilder = this,
            descriptorPool = descriptorPool,
            descriptorSetLayout = descriptorSetLayout,
            setCount = setCount,
        )
        return Result(descriptorSetLayout, descriptorSet)
    }

    /**
     * Represents a single binding in a descriptor set.
     *
     * @property descriptorType The type of descriptor (uniform buffer, sampler, etc.)
     * @property descriptorCount Number of descriptors in this binding
     * @property stages Shader stages where this binding will be accessible
     */
    data class Binding(
        var descriptorType: DescriptorType,
        var descriptorCount: Int,
        var stages: EnumSet<ShaderStage>,
    )

    /**
     * Enumeration of supported Vulkan descriptor types.
     *
     * @property vkValue The corresponding Vulkan API constant value
     */
    enum class DescriptorType(
        val vkValue: Int,
    ) {
        /**
         * Represents a sampler object that defines how texture images should be sampled.
         * Used for configuring texture filtering, addressing modes, and other sampling parameters.
         * This type is typically used in conjunction with separate image descriptors.
         */
        SAMPLER(VK10.VK_DESCRIPTOR_TYPE_SAMPLER),

        /**
         * Combines both a texture image and a sampler into a single descriptor.
         * This is the most common type used for texture sampling in shaders.
         * Provides a more convenient way to handle texture sampling compared to separate
         * image and sampler descriptors.
         */
        COMBINED_IMAGE_SAMPLER(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER),

        /**
         * Represents a texture image resource that can be sampled in shaders.
         * Used when you want to separate the image and sampler descriptors.
         * Allows for more flexible texture sampling configurations where different
         * samplers can be used with the same image.
         */
        SAMPLED_IMAGE(VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE),

        /**
         * Describes an image that can be used for storage operations in shaders.
         * Allows both reading and writing to the image in shader code.
         * Commonly used in compute shaders for image processing operations
         * and general-purpose GPU computing tasks.
         */
        STORAGE_IMAGE(VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE),

        /**
         * Represents a buffer that can be accessed as an array of texels in shaders.
         * Provides read-only access to formatted buffer data.
         * Useful for storing and accessing large arrays of structured data
         * that need to be accessed in a formatted way.
         */
        UNIFORM_TEXEL_BUFFER(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER),

        /**
         * Describes a buffer that can be read and written as texels in shaders.
         * Allows both reading and writing operations on formatted buffer data.
         * Commonly used in compute shaders for processing structured data
         * where both read and write access is needed.
         */
        STORAGE_TEXEL_BUFFER(VK10.VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER),

        /**
         * Represents a uniform buffer object (UBO) containing read-only data.
         * Used for storing small amounts of frequently accessed shader data.
         * Typically used for per-frame or per-object constant data
         * that needs to be accessed efficiently in shaders.
         */
        UNIFORM_BUFFER(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER),

        /**
         * Describes a storage buffer object (SSBO) for shader read/write operations.
         * Allows both reading and writing of buffer data in shaders.
         * Suitable for larger data sets and when shader write access is required.
         * Commonly used in compute shaders for general-purpose computing.
         */
        STORAGE_BUFFER(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER),

        /**
         * Represents a dynamic uniform buffer with configurable offset.
         * Similar to UNIFORM_BUFFER but allows changing the offset at binding time.
         * Enables efficient use of a single large buffer for multiple draw calls
         * by adjusting the offset without recreating descriptor sets.
         */
        UNIFORM_BUFFER_DYNAMIC(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC),
    }

    data class Result(
        val descriptorSetLayout: DescriptorSetLayout,
        val descriptorSet: DescriptorSet,
    )
}
