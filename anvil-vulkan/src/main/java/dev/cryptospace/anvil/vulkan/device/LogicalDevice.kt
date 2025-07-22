package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.SwapChainFactory
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue

/**
 * Represents a logical Vulkan device that provides the main interface for interacting with a physical GPU.
 * Manages device-specific resources, queues and provides functionality for creating swap chains.
 *
 * @property vulkan The parent VulkanRenderingSystem instance that owns this device
 * @property handle The native Vulkan device handle
 * @property deviceSurfaceInfo Information about the physical device and its surface capabilities
 */
data class LogicalDevice(
    val vulkan: VulkanRenderingSystem,
    val handle: VkDevice,
    val deviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
) : NativeResource() {

    /** The physical device (GPU) associated with this logical device */
    val physicalDevice: PhysicalDevice = deviceSurfaceInfo.physicalDevice

    /** Queue used for submitting graphics commands to the GPU */
    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, deviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    /** Queue used for presenting rendered images to the surface */
    val presentQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, deviceSurfaceInfo.presentQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    /**
     * Creates a new swap chain for this device based on the surface capabilities.
     * The swap chain manages the queue of images that can be presented to the surface.
     *
     * **Note**: The caller is responsible for freeing the swap chain resources when no longer needed.
     *
     * @return A new SwapChain instance
     */
    fun createSwapChain(renderPass: RenderPass): SwapChain = SwapChainFactory.create(this, renderPass)

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
        vkDestroyDevice(handle, null)
    }
}
