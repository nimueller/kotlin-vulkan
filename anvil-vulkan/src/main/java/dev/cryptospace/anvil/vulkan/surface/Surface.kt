package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.window.Window
import dev.cryptospace.anvil.vulkan.handle.VkSurface
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.VkInstance

/**
 * Represents a Vulkan surface that enables rendering to a window system.
 * A surface acts as a bridge between Vulkan and the window system, allowing
 * graphics to be displayed. The surface is owned by a Vulkan instance and
 * must be properly destroyed when no longer needed.
 */
data class Surface(
    val vulkanInstance: VkInstance,
    val window: Window,
    val handle: VkSurface,
) : NativeResource() {

    /**
     * Creates a Surface instance for the specified window using the given Vulkan instance.
     *
     * @param vulkanInstance The Vulkan instance that will own the surface
     * @param window The window for which to create the surface
     * @throws IllegalStateException if surface creation fails
     */
    constructor(vulkanInstance: VkInstance, window: Window) : this(
        vulkanInstance,
        window,
        createWindowSurface(vulkanInstance, window),
    )

    override fun destroy() {
        vkDestroySurfaceKHR(vulkanInstance, handle.value, null)
    }

    companion object {

        /**
         * Creates a Vulkan surface for the specified window.
         * The surface acts as a bridge between Vulkan and the window system, enabling
         * rendering of Vulkan graphics to the window. The created surface is owned by
         * the Vulkan instance and must be destroyed when no longer needed.
         *
         * @param vulkanInstance The Vulkan instance that will own the created surface
         * @param window The window for which to create the surface
         * @return A new Surface instance wrapped in a resource management class
         * @throws IllegalStateException if the surface creation fails due to system or resource limitations
         */
        private fun createWindowSurface(vulkanInstance: VkInstance, window: Window): VkSurface =
            MemoryStack.stackPush().use { stack ->
                val pSurface = stack.mallocLong(1)
                val result = GLFWVulkan.glfwCreateWindowSurface(vulkanInstance, window.handle.value, null, pSurface)
                check(result == 0) { "Failed to create window surface" }
                return VkSurface(pSurface[0])
            }
    }
}
