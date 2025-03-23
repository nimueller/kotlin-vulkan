package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.window.Window
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR

data class VulkanSurface(val vulkan: Vulkan, val window: Window, val handle: Long) : NativeResource() {

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan instance already destroyed" }
        check(window.isAlive) { "Window instance is already destroyed" }
        vkDestroySurfaceKHR(vulkan.instance, handle, null)
    }
}
