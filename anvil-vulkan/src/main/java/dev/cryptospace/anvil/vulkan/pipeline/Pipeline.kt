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
    /** Handle to the pipeline layout defining descriptor set layouts and push constants */
    val pipelineLayoutHandle: VkPipelineLayout,
    /** Native handle to the Vulkan graphics pipeline object */
    val handle: VkPipeline,
) : NativeResource() {

    /**
     * Destroys the graphics pipeline and its associated pipeline layout.
     * This releases all Vulkan resources associated with this pipeline.
     */
    override fun destroy() {
        VK10.vkDestroyPipeline(logicalDevice.handle, handle.value, null)
        VK10.vkDestroyPipelineLayout(logicalDevice.handle, pipelineLayoutHandle.value, null)
    }
}
