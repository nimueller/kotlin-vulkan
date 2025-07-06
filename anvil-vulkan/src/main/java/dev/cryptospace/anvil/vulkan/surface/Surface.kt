package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.window.Window
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR

data class Surface(
    val vulkan: VulkanRenderingSystem,
    val window: Window,
    val address: Address,
) : NativeResource() {

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan instance already destroyed" }
        check(window.isAlive) { "Window instance is already destroyed" }
        vkDestroySurfaceKHR(vulkan.instance, address.handle, null)
    }
}
