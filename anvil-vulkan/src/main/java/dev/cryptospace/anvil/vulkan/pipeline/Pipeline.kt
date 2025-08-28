package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.graphics.CommandBuffer
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.VkDescriptorSet
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10

/**
 * Represents a Vulkan graphics pipeline that defines the rendering state and shader stages.
 *
 * The graphics pipeline encapsulates the complete state needed for rendering operations,
 * including shaders, vertex layouts, and another fixed-function state.
 */
class Pipeline(
    /** The logical device that this pipeline is associated with */
    val logicalDevice: LogicalDevice,
    /** Handle to the pipeline layout defining descriptor set layouts and push constants */
    val pipelineLayoutHandle: VkPipelineLayout,
    /** Native handle to the Vulkan graphics pipeline object */
    val handle: VkPipeline,
) : NativeResource() {

    /**
     * Binds this pipeline and its descriptor sets to the specified command buffer for rendering.
     * This method must be called before issuing any draw commands that use this pipeline.
     *
     * @param stack The memory stack used for allocating temporary buffers
     * @param commandBuffer The command buffer to record the binding commands into
     * @param descriptorSets List of descriptor sets containing resource bindings (textures, uniforms, etc.)
     */
    fun bind(stack: MemoryStack, commandBuffer: CommandBuffer, descriptorSets: List<VkDescriptorSet>) {
        val descriptorSetHandles = stack.mallocLong(descriptorSets.size)

        for (descriptorSet in descriptorSets) {
            descriptorSetHandles.put(descriptorSet.value)
        }

        descriptorSetHandles.flip()

        VK10.vkCmdBindPipeline(commandBuffer.handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, handle.value)
        VK10.vkCmdBindDescriptorSets(
            commandBuffer.handle,
            VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
            pipelineLayoutHandle.value,
            0,
            descriptorSetHandles,
            null,
        )
    }

    /**
     * Destroys the graphics pipeline and its associated pipeline layout.
     * This releases all Vulkan resources associated with this pipeline.
     */
    override fun destroy() {
        VK10.vkDestroyPipeline(logicalDevice.handle, handle.value, null)
        VK10.vkDestroyPipelineLayout(logicalDevice.handle, pipelineLayoutHandle.value, null)
    }
}
