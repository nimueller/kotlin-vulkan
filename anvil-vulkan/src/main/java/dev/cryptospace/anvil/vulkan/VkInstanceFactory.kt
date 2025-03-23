package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.pushStringList
import dev.cryptospace.anvil.core.putAllStrings
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

object VkInstanceFactory {

    @JvmStatic
    private val logger = logger<VkInstanceFactory>()

    fun createInstance(
        glfw: Glfw,
        validationLayers: VulkanValidationLayers
    ): VkInstance = MemoryStack.stackPush().use { stack ->
        val appInfo = stack.createApplicationInfo()
        val createInfo = stack.createInstanceCreateInfo(glfw, appInfo, validationLayers)
        val instance = stack.createVulkanInstance(createInfo)

        return VkInstance(instance[0], createInfo)
    }

    private fun MemoryStack.createApplicationInfo(): VkApplicationInfo {
        val appInfo = VkApplicationInfo.calloc(this).apply {
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

    private fun MemoryStack.createInstanceCreateInfo(
        glfw: Glfw,
        appInfo: VkApplicationInfo,
        validationLayers: VulkanValidationLayers
    ): VkInstanceCreateInfo {
        val windowSystemExtensions = glfw.getRequiredVulkanExtensions()
        val additionalExtensions = getAdditionalVulkanExtensions()

        val extensions = mallocPointer(windowSystemExtensions.size + additionalExtensions.size)
        extensions.putAllStrings(this, windowSystemExtensions)
        extensions.putAllStrings(this, additionalExtensions)
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

    private fun getAdditionalVulkanExtensions(): List<String> {
        if (!AppConfig.useValidationLayers) {
            return emptyList()
        }

        return AppConfig.validationLayers
    }

    private fun MemoryStack.createVulkanInstance(createInfo: VkInstanceCreateInfo): PointerBuffer {
        val instance = mallocPointer(1)
        val result = vkCreateInstance(createInfo, null, instance)
        check(result == VK_SUCCESS) { "Failed to create Vulkan instance: $result" }
        return instance
    }

}
