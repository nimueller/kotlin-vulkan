package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.Handle
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.surface.Surface
import dev.cryptospace.anvil.vulkan.surface.SurfaceSwapChainDetails
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR

object SwapChainFactory {

    fun create(logicalDevice: LogicalDevice, renderPass: RenderPass): SwapChain = MemoryStack.stackPush().use { stack ->
        val deviceSurfaceInfo = logicalDevice.deviceSurfaceInfo
        val surface = deviceSurfaceInfo.surface
        val swapChainDetails = deviceSurfaceInfo.swapChainDetails

        val createInfo = createSwapChainCreateInfo(
            stack,
            surface,
            deviceSurfaceInfo,
            swapChainDetails,
        )

        val pointer = stack.mallocLong(1)
        vkCreateSwapchainKHR(
            logicalDevice.handle,
            createInfo,
            null,
            pointer,
        ).validateVulkanSuccess()

        SwapChain(logicalDevice, Handle(pointer[0]), renderPass)
    }

    private fun createSwapChainCreateInfo(
        stack: MemoryStack,
        surface: Surface,
        deviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
        swapChainDetails: SurfaceSwapChainDetails,
    ): VkSwapchainCreateInfoKHR = VkSwapchainCreateInfoKHR.calloc(stack).apply {
        val surfaceCapabilities = swapChainDetails.surfaceCapabilities
        var imageCount = surfaceCapabilities.minImageCount() + 1

        if (surfaceCapabilities.maxImageCount() > 0 && imageCount > surfaceCapabilities.maxImageCount()) {
            imageCount = surfaceCapabilities.maxImageCount()
        }
        val graphicsQueueFamilyIndex = deviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex
        val presentQueueFamilyIndex = deviceSurfaceInfo.presentQueueFamilyIndex

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
}
