package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.queryVulkanBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR
import org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR

/**
 * Represents a Vulkan swap chain, which manages a collection of presentable images that can be
 * displayed on the screen. The swap chain is responsible for image presentation and synchronization
 * with the display's refresh rate.
 *
 * The swap chain is closely tied to both the surface and the physical device's presentation capabilities,
 * handling details such as
 * - Image format and color space
 * - Number of images in the chain
 * - Image presentation mode
 * - Image dimensions
 *
 * @param logicalDevice The logical device that owns this swap chain
 * @param handle Native handle to the Vulkan swap chain object
 */
data class SwapChain(
    val logicalDevice: LogicalDevice,
    val handle: Handle,
) : NativeResource() {

    val images = MemoryStack.stackPush().use { stack ->
        stack.queryVulkanBuffer(
            bufferInitializer = { size -> stack.mallocLong(size) },
            bufferQuery = { countBuffer, resultBuffer ->
                vkGetSwapchainImagesKHR(logicalDevice.handle, handle.value, countBuffer, resultBuffer)
            },
        )
    }

    override fun destroy() {
        vkDestroySwapchainKHR(logicalDevice.handle, handle.value, null)
    }
}
