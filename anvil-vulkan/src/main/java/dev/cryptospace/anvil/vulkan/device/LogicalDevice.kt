package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.DescriptorPool
import dev.cryptospace.anvil.vulkan.graphics.DescriptorSetLayout
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue

/**
 * Represents a logical Vulkan device that provides the main interface for interacting with a physical GPU.
 * Manages device-specific resources, queues and provides functionality for creating swap chains.
 *
 * @property handle The native Vulkan device handle
 * @property physicalDeviceSurfaceInfo Information about the physical device and its surface capabilities
 */
data class LogicalDevice(
    val handle: VkDevice,
    val physicalDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
) : NativeResource() {

    /** The physical device (GPU) associated with this logical device */
    val physicalDevice: PhysicalDevice = physicalDeviceSurfaceInfo.physicalDevice

    /** Queue used for submitting graphics commands to the GPU */
    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDeviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    /** Queue used for presenting rendered images to the surface */
    val presentQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDeviceSurfaceInfo.presentQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    val descriptorPool: DescriptorPool = DescriptorPool(this, VulkanRenderingSystem.FRAMES_IN_FLIGHT)

    val descriptorSetLayout: DescriptorSetLayout = DescriptorSetLayout(this)

    val renderPass: RenderPass = RenderPass(this)

    val graphicsPipeline: GraphicsPipeline = GraphicsPipeline(this, renderPass)

    /**
     * The swap chain managing presentation of rendered images to the surface.
     * Created from the logical device and handles image acquisition, rendering synchronization,
     * and presentation to the display using the selected surface format and present mode.
     * The swap chain dimensions and properties are determined based on the physical device's
     * surface capabilities and the window size.
     */
    var swapChain: SwapChain = SwapChain(this, renderPass)

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    val commandPool: CommandPool = CommandPool(this)

    fun recreateSwapChain() {
        swapChain = swapChain.recreate(renderPass)
    }

    override fun destroy() {
        commandPool.close()
        swapChain.close()
        graphicsPipeline.close()
        renderPass.close()
        descriptorSetLayout.close()
        descriptorPool.close()

        vkDestroyDevice(handle, null)
    }
}
