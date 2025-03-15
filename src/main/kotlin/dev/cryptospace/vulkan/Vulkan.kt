package dev.cryptospace.vulkan

import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import sun.java2d.StateTrackableDelegate.createInstance

class Vulkan : AutoCloseable {

    val instance: VkInstance by lazy { createInstance() }

    private fun createInstance(): VkInstance = MemoryStack.stackPush().use { stack ->
        val appInfo = VkApplicationInfo.malloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            pApplicationName(stack.ASCII("Hello Vulkan Application"))
            applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            pEngineName(stack.ASCII("Vulkan"))
            engineVersion(VK_MAKE_VERSION(1, 0, 0))
            apiVersion(VK_MAKE_VERSION(1, 0, 0))
        }

        val glfwExtensions = glfwGetRequiredInstanceExtensions()

        checkNotNull(glfwExtensions) { "Unable to get GLFW Vulkan extensions" }

        val createInfo = VkInstanceCreateInfo.malloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            pApplicationInfo(appInfo)
            ppEnabledLayerNames(stack.mallocPointer(0))
            ppEnabledExtensionNames(glfwExtensions)
        }

        val instance = stack.mallocPointer(1)
        val result = vkCreateInstance(createInfo, null, instance)
        check(result == VK_SUCCESS) { "Failed to create Vulkan instance: $result" }

        return VkInstance(instance.get(), createInfo)
    }

    override fun close() {
        vkDestroyInstance(instance, null)
    }

}
