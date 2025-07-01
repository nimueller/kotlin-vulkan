package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.pushStringList
import dev.cryptospace.anvil.core.putAllStrings
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

object VkInstanceFactory {

    @JvmStatic
    private val logger = logger<VkInstanceFactory>()

    fun createInstance(glfw: Glfw, validationLayers: VulkanValidationLayers): VkInstance =
        MemoryStack.stackPush().use { stack ->
            val appInfo = createApplicationInfo(stack)
            val createInfo = createInstanceCreateInfo(stack, glfw, appInfo, validationLayers)
            val instance = createVulkanInstance(stack, createInfo)

            return VkInstance(instance[0], createInfo)
        }

    private fun createApplicationInfo(stack: MemoryStack): VkApplicationInfo = VkApplicationInfo.calloc(stack).apply {
        sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
        pNext(NULL)
        pApplicationName(stack.ASCII("Hello Vulkan Application"))
        applicationVersion(VK_MAKE_VERSION(1, 0, 0))
        pEngineName(stack.ASCII("Vulkan"))
        engineVersion(VK_MAKE_VERSION(1, 0, 0))
        apiVersion(VK_MAKE_VERSION(1, 0, 0))
    }

    private fun createInstanceCreateInfo(
        stack: MemoryStack,
        glfw: Glfw,
        appInfo: VkApplicationInfo,
        validationLayers: VulkanValidationLayers,
    ): VkInstanceCreateInfo {
        val windowSystemExtensions = glfw.getRequiredVulkanExtensions()
        val additionalExtensions = getAdditionalVulkanExtensions(validationLayers)

        val extensions = stack.callocPointer(windowSystemExtensions.size + additionalExtensions.size)
        extensions.putAllStrings(stack, windowSystemExtensions)
        extensions.putAllStrings(stack, additionalExtensions)
        extensions.flip()

        val layerNames = validationLayers.requestedLayerNames
        logger.info("Using validation layers: $layerNames")
        val layers = stack.pushStringList(layerNames)

        val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            pApplicationInfo(appInfo)
            ppEnabledLayerNames(layers)
            ppEnabledExtensionNames(extensions)
        }

        return createInfo
    }

    private fun getAdditionalVulkanExtensions(validationLayers: VulkanValidationLayers): List<String> {
        if (!AppConfig.useValidationLayers) {
            return emptyList()
        }

        return validationLayers.requestedLayerNames
    }

    private fun createVulkanInstance(stack: MemoryStack, createInfo: VkInstanceCreateInfo): PointerBuffer {
        val instance = stack.callocPointer(1)
        vkCreateInstance(createInfo, null, instance).validateVulkanSuccess()
        return instance
    }
}
