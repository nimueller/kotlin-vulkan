package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR

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
    fun createSwapChain(renderPass: RenderPass): SwapChain = MemoryStack.stackPush().use { stack ->
        val surface = deviceSurfaceInfo.surface
        val swapChainDetails = deviceSurfaceInfo.swapChainDetails
        val surfaceCapabilities = swapChainDetails.surfaceCapabilities
        var imageCount = surfaceCapabilities.minImageCount() + 1

        if (surfaceCapabilities.maxImageCount() > 0 && imageCount > surfaceCapabilities.maxImageCount()) {
            imageCount = surfaceCapabilities.maxImageCount()
        }

        val graphicsQueueFamilyIndex = deviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex
        val presentQueueFamilyIndex = deviceSurfaceInfo.presentQueueFamilyIndex

        val createInfo = VkSwapchainCreateInfoKHR.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
            surface(surface.handle.value)
            minImageCount(imageCount)
            imageFormat(swapChainDetails.bestSurfaceFormat.format())
            imageColorSpace(swapChainDetails.bestSurfaceFormat.colorSpace())
            imageExtent(swapChainDetails.swapChainExtent)
            imageArrayLayers(1)
            imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)

            if (graphicsQueueFamilyIndex != presentQueueFamilyIndex) {
                imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                queueFamilyIndexCount(2)
                pQueueFamilyIndices(stack.ints(graphicsQueueFamilyIndex, presentQueueFamilyIndex))
            } else {
                imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                queueFamilyIndexCount(0)
                pQueueFamilyIndices(null)
            }

            preTransform(surfaceCapabilities.currentTransform())
            compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            presentMode(swapChainDetails.bestPresentMode.vulkanValue)
            clipped(true)
            oldSwapchain(VK_NULL_HANDLE)
        }

        val pointer = stack.mallocLong(1)
        vkCreateSwapchainKHR(
            handle,
            createInfo,
            null,
            pointer,
        ).validateVulkanSuccess()

        SwapChain(this, Handle(pointer[0]), renderPass)
    }

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
        vkDestroyDevice(handle, null)
    }
}
