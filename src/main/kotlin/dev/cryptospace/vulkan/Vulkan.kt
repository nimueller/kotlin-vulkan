package dev.cryptospace.vulkan

import dev.cryptospace.vulkan.core.VkInstanceFactory
import dev.cryptospace.vulkan.core.VulkanValidationLayerLogger
import dev.cryptospace.vulkan.core.VulkanValidationLayers
import org.lwjgl.vulkan.VK10.vkDestroyInstance

const val VK_KHRONOS_VALIDATION_LAYER_NAME = "VK_LAYER_KHRONOS_validation"

class Vulkan(
    private val useValidationLayers: Boolean,
    validationLayerNames: List<String>
) : AutoCloseable {

    val validationLayers = VulkanValidationLayers(if (useValidationLayers) validationLayerNames else emptyList())
    val instanceFactory = VkInstanceFactory(useValidationLayers, validationLayers)
    val instance = instanceFactory.createInstance()
    val validationLayerLogger = VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }

    override fun close() {
        if (useValidationLayers) {
            validationLayerLogger.destroy()
        }

        vkDestroyInstance(instance, null)
    }

}
