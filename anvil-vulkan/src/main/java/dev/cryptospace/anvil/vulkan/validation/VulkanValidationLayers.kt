package dev.cryptospace.anvil.vulkan.validation

import dev.cryptospace.anvil.core.logger
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties
import org.lwjgl.vulkan.VkLayerProperties

class VulkanValidationLayers(val requestedLayerNames: List<String>) {

    val supportedLayerNames: List<String> = getSupportedLayers()

    init {
        validateRequestedLayersSupported()
    }

    private fun getSupportedLayers(): List<String> = MemoryStack.stackPush().use { stack ->
        val layerCount = stack.mallocInt(1)
        var result = vkEnumerateInstanceLayerProperties(layerCount, null)
        check(result == VK_SUCCESS) { "Failed to enumerate Vulkan instance layer properties: $result" }

        if (layerCount[0] == 0) {
            logger.warn("Found no supported Vulkan instance layers")
            return emptyList()
        }

        val supportedLayerNames = VkLayerProperties.malloc(layerCount[0])
        result = vkEnumerateInstanceLayerProperties(layerCount, supportedLayerNames)
        check(result == VK_SUCCESS) { "Failed to enumerate Vulkan instance layer properties: $result" }
        return supportedLayerNames.toList().map { it.layerNameString() }
    }

    private fun validateRequestedLayersSupported() {
        val requestButNotSupported = mutableListOf<String>()

        requestedLayerNames.forEach { requestedLayerName ->
            val layerFound = supportedLayerNames.any { it == requestedLayerName }

            if (!layerFound) {
                requestButNotSupported.add(requestedLayerName)
            }
        }

        logger.info("Following validation layers are supported: $supportedLayerNames")
        check(requestButNotSupported.isEmpty()) { "Requested unsupported validation layers: $requestButNotSupported" }
    }

    companion object {

        @JvmStatic
        private val logger = logger<VulkanValidationLayers>()
    }

}
