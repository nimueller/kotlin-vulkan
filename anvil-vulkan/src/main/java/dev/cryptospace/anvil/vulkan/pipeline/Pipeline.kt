package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
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
    /** Handle to the pipeline layout defining descriptor set layouts and push constants. Not owned by the pipeline. */
    val pipelineLayoutHandle: VkPipelineLayout,
    /** Native handle to the Vulkan graphics pipeline object */
    val handle: VkPipeline,
) : NativeResource() {

    /**
     * Destroys the graphics pipeline. The pipeline layout is shared between pipelines and destroyed by its owner.
     */
    override fun destroy() {
        VK10.vkDestroyPipeline(logicalDevice.handle, handle.value, null)
    }
}
