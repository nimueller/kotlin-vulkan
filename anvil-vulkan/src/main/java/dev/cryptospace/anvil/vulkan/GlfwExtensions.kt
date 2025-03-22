package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.toStringList
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.system.MemoryStack

fun Glfw.getRequiredVulkanExtensions(): List<String> {
    val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()
    checkNotNull(glfwExtensions) { "Failed to find list of required Vulkan extensions" }
    val extensionNames = glfwExtensions.toStringList()
    return extensionNames
}

fun Window.createSurface(): VulkanSurface {
    MemoryStack.stackPush().use { stack ->
        val surfaceBuffer = stack.mallocLong(1)
        val result = glfwCreateWindowSurface(Vulkan.instance, this.handle, null, surfaceBuffer)
        check(result == 0) { "Failed to create window surface" }
        return VulkanSurface(window = this, handle = surfaceBuffer[0])
    }
}
