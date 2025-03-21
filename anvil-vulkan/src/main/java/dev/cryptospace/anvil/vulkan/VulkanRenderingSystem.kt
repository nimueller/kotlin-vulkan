package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.AppConfig.useValidationLayers
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.WindowSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import dev.cryptospace.vulkan.core.device.LogicalDevice
import dev.cryptospace.vulkan.core.device.PhysicalDevice
import dev.cryptospace.vulkan.core.device.PhysicalDevice.Companion.pickBestDevice
import org.lwjgl.vulkan.VK10.vkDestroyInstance

class VulkanRenderingSystem(windowSystem: WindowSystem) : RenderingSystem {

    private val validationLayers =
        VulkanValidationLayers(if (useValidationLayers) AppConfig.validationLayers else emptyList())
    private val instance = VkInstanceFactory(windowSystem).createInstance(validationLayers)
    private val validationLayerLogger = VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }
    private val physicalDevices = PhysicalDevice.listPhysicalDevices(instance).also { physicalDevices ->
        logger.info("Found physical devices: $physicalDevices")
    }
    private val physicalDevice = physicalDevices.pickBestDevice().also { physicalDevice ->
        logger.info("Selected best physical device: $physicalDevice")
    }
    private val logicalDevice = LogicalDevice.create(physicalDevice)

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
        private val logger = logger<VulkanRenderingSystem>()
    }

}
