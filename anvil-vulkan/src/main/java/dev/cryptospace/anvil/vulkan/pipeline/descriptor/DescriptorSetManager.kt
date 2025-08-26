package dev.cryptospace.anvil.vulkan.pipeline.descriptor

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.MAX_TEXTURE_COUNT
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem.Companion.FRAMES_IN_FLIGHT
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderStage
import java.util.EnumSet

class DescriptorSetManager(
    logicalDevice: LogicalDevice,
) : NativeResource() {

    /**
     * Descriptor pool for allocating descriptor sets used in the rendering pipeline.
     * This pool manages the allocation of descriptor sets for both per-frame uniforms and material textures.
     * Configured with capacity for frame-in-flight buffers plus maximum allowed texture descriptors.
     * Supports dynamic updating of descriptors after binding for texture updates during runtime.
     */
    private val descriptorPool: DescriptorPool =
        DescriptorPool(
            logicalDevice = logicalDevice,
            frameInFlights = FRAMES_IN_FLIGHT,
            maxTextureCount = MAX_TEXTURE_COUNT,
        )

    /**
     * Descriptor set for frame-specific uniform buffers.
     * Contains descriptor sets allocated for each frame in flight,
     * managing per-frame uniform buffer bindings used for view/projection matrices
     * and other frame-specific data.
     */
    val frameDescriptorSet: DescriptorSetBuilder.Result =
        DescriptorSetBuilder()
            .binding(
                descriptorType = DescriptorSetBuilder.DescriptorType.UNIFORM_BUFFER,
                descriptorCount = 1,
                stages = EnumSet.of(ShaderStage.VERTEX),
            )
            .build(
                logicalDevice = logicalDevice,
                descriptorPool = descriptorPool,
                setCount = FRAMES_IN_FLIGHT,
            )

    /**
     * Layout for texture descriptor bindings used in shaders.
     * Defines the structure for combined image samplers that allow textures
     * to be accessed in fragment shaders. This layout supports dynamic texture
     * updates and variable descriptor counts up to MAX_TEXTURE_COUNT.
     * Created with an update-after-bind flag to allow runtime texture updates.
     */
    val textureDescriptorSet: DescriptorSetBuilder.Result =
        DescriptorSetBuilder()
            .variableBinding(
                descriptorType = DescriptorSetBuilder.DescriptorType.COMBINED_IMAGE_SAMPLER,
                descriptorCount = MAX_TEXTURE_COUNT,
                stages = EnumSet.of(ShaderStage.FRAGMENT),
            )
            .build(
                logicalDevice = logicalDevice,
                descriptorPool = descriptorPool,
                setCount = 1,
            )

    override fun destroy() {
        frameDescriptorSet.descriptorSetLayout.close()
        textureDescriptorSet.descriptorSetLayout.close()
        descriptorPool.close()
    }
}
