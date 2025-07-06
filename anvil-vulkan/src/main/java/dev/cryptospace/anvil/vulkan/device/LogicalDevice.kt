package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
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

data class LogicalDevice(
    val vulkan: VulkanRenderingSystem,
    val handle: VkDevice,
    val deviceSurfaceInfo: PhysicalDeviceSurfaceInfo,
) : NativeResource() {

    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, deviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }
    val presentQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, deviceSurfaceInfo.presentQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    fun createSwapChain(): SwapChain = MemoryStack.stackPush().use { stack ->
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
            surface(surface.address.handle)
            minImageCount(imageCount)
            imageFormat(swapChainDetails.bestSurfaceFormat.format())
            imageColorSpace(swapChainDetails.bestSurfaceFormat.colorSpace())
            imageExtent(swapChainDetails.swapExtent)
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

        SwapChain(this, Address(pointer[0]))
    }

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
        vkDestroyDevice(handle, null)
    }
}
