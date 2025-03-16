package dev.cryptospace.vulkan.core

import dev.cryptospace.vulkan.core.device.LogicalDevice
import dev.cryptospace.vulkan.core.device.PhysicalDevice
import dev.cryptospace.vulkan.core.device.PhysicalDevice.Companion.pickBestDevice
import dev.cryptospace.vulkan.utils.getLogger
import org.lwjgl.vulkan.VK10.vkDestroyInstance

const val VK_KHRONOS_VALIDATION_LAYER_NAME = "VK_LAYER_KHRONOS_validation"

class Vulkan(
    private val useValidationLayers: Boolean,
    validationLayerNames: List<String>
) : AutoCloseable {

    val validationLayers = VulkanValidationLayers(if (useValidationLayers) validationLayerNames else emptyList())
    val instance = VkInstanceFactory.createInstance(validationLayers)
    val validationLayerLogger = VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }
    val physicalDevices = PhysicalDevice.listPhysicalDevices(instance).also { physicalDevices ->
        logger.info("Found physical devices: $physicalDevices")
    }
    val physicalDevice = physicalDevices.pickBestDevice().also { physicalDevice ->
        logger.info("Selected best physical device: $physicalDevice")
    }
    val logicalDevice = LogicalDevice.create(physicalDevice)

    override fun close() {
        if (useValidationLayers) {
            validationLayerLogger.destroy()
        }

        physicalDevices.forEach { it.close() }
        logicalDevice.close()
        vkDestroyInstance(instance, null)
    }

    companion object {

        @JvmStatic
        private val logger = getLogger<Vulkan>()
    }

}
