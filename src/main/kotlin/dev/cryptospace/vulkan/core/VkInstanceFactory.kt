package dev.cryptospace.vulkan.core

import dev.cryptospace.vulkan.Vulkan
import dev.cryptospace.vulkan.utils.getLogger
import dev.cryptospace.vulkan.utils.pushStringList
import dev.cryptospace.vulkan.utils.pushStrings
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

class VkInstanceFactory(
    private val useValidationLayers: Boolean,
    private val validationLayers: VulkanValidationLayers
) {
    fun createInstance(): VkInstance = MemoryStack.stackPush().use { stack ->
        val appInfo = stack.createApplicationInfo()
        val createInfo = stack.createInstanceCreateInfo(appInfo)
        val instance = stack.createVulkanInstance(createInfo)

        return VkInstance(instance[0], createInfo)
    }

    private fun MemoryStack.createApplicationInfo(): VkApplicationInfo {
        val appInfo = VkApplicationInfo.malloc(this).apply {
            sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            pApplicationName(ASCII("Hello Vulkan Application"))
            applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            pEngineName(ASCII("Vulkan"))
            engineVersion(VK_MAKE_VERSION(1, 0, 0))
            apiVersion(VK_MAKE_VERSION(1, 0, 0))
            pNext(VK_NULL_HANDLE)
        }
        return appInfo
    }

    private fun MemoryStack.createInstanceCreateInfo(appInfo: VkApplicationInfo): VkInstanceCreateInfo {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()
        checkNotNull(glfwExtensions) { "Unable to get GLFW Vulkan extensions" }

        val additionalExtensions =
            if (useValidationLayers) pushStrings(VK_EXT_DEBUG_UTILS_EXTENSION_NAME) else pointers(0)
        val extensions = mallocPointer(glfwExtensions.limit() + additionalExtensions.limit())
        extensions.put(glfwExtensions)
        extensions.put(additionalExtensions)
        extensions.flip()

        val layerNames = validationLayers.requestedLayerNames
        logger.info("Using validation layers: $layerNames")
        val layers = pushStringList(layerNames)

        val createInfo = VkInstanceCreateInfo.malloc(this).apply {
            sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            pApplicationInfo(appInfo)
            ppEnabledLayerNames(layers)
            ppEnabledExtensionNames(extensions)
        }

        return createInfo
    }

    private fun MemoryStack.createVulkanInstance(createInfo: VkInstanceCreateInfo): PointerBuffer {
        val instance = mallocPointer(1)
        val result = vkCreateInstance(createInfo, null, instance)
        check(result == VK_SUCCESS) { "Failed to create Vulkan instance: $result" }
        return instance
    }

    companion object {
        @JvmStatic
        private val logger = getLogger<Vulkan>()
    }

}
