package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.native.NativeResource
import org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR

data class SwapChain(
    val logicalDevice: LogicalDevice,
    val address: Address,
) : NativeResource() {

    override fun destroy() {
        vkDestroySwapchainKHR(logicalDevice.handle, address.handle, null)
    }
}
