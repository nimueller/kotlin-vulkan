package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.toStringList
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack

/**
 * Gets the list of Vulkan extensions required by GLFW.
 * These extensions are necessary for Vulkan to work with GLFW windows.
 *
 * @return List of required Vulkan extension names as strings
 * @throws IllegalStateException if the required extensions cannot be retrieved
 */
fun Glfw.getRequiredVulkanExtensions(): List<String> {
    val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
    checkNotNull(glfwExtensions) { "Failed to find list of required Vulkan extensions" }
    val extensionNames = glfwExtensions.toStringList()
    return extensionNames
}

/**
 * Creates a Vulkan surface for this window.
 * The surface is used for rendering Vulkan graphics to the window.
 *
 * @param vulkan The Vulkan instance to create the surface for
 * @return A new Surface instance
 * @throws IllegalStateException if the surface creation fails
 */
fun Window.createSurface(vulkan: VulkanRenderingSystem): Surface {
    MemoryStack.stackPush().use { stack ->
        val surfaceBuffer = stack.mallocLong(1)
        val result = GLFWVulkan.glfwCreateWindowSurface(vulkan.instance, this.address.handle, null, surfaceBuffer)
        check(result == 0) { "Failed to create window surface" }
        return Surface(vulkan = vulkan, window = this, address = Address(surfaceBuffer[0]))
    }
}

/**
 * Gets the size of the framebuffer for this window.
 * The framebuffer size represents the actual number of pixels that can be rendered to.
 * On high-DPI displays (like Retina), the framebuffer size may be larger than the window size
 * due to DPI scaling. For example, a window that is 800x600 pixels might have a 1600x1200
 * framebuffer on a display with 2x scaling.
 *
 * @return the framebuffer size
 */
fun Window.getFramebufferSize(): FramebufferSize {
    MemoryStack.stackPush().use { stack ->
        val widthBuffer = stack.mallocInt(1)
        val heightBuffer = stack.mallocInt(1)
        glfwGetFramebufferSize(address.handle, widthBuffer, heightBuffer)
        return FramebufferSize(widthBuffer[0], heightBuffer[0])
    }
}

data class FramebufferSize(
    val width: Int,
    val height: Int,
)
