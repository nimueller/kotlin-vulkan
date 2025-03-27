package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.AppConfig.useValidationLayers
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.device.LogicalDeviceFactory
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice.Companion.pickBestDevice
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.vulkan.VK10.vkDestroyInstance

class Vulkan(glfw: Glfw) : RenderingSystem() {

    private val validationLayers =
        VulkanValidationLayers(if (useValidationLayers) AppConfig.validationLayers else emptyList())

    val instance = VkInstanceFactory.createInstance(glfw, validationLayers)

    private val validationLayerLogger = VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }

    private val surface = glfw.window.createSurface(this).also { surface ->
        logger.info("Created surface: $surface")
    }

    private val physicalDevices = PhysicalDevice.listPhysicalDevices(this).also { physicalDevices ->
        physicalDevices.forEach { physicalDevice ->
            physicalDevice.initSurface(surface)
        }

        logger.info("Found physical devices: $physicalDevices")
    }

    private val physicalDevice = physicalDevices.pickBestDevice(surface).also { physicalDevice ->
        logger.info("Selected best physical device: $physicalDevice")
    }

    private val logicalDevice = LogicalDeviceFactory.create(this, physicalDevice, surface)

    override fun destroy() {
        physicalDevices.forEach { it.close() }
        logicalDevice.close()

        surface.close()

        if (useValidationLayers) {
            validationLayerLogger.destroy()
        }

        vkDestroyInstance(instance, null)
    }

    override fun toString(): String {
        return "Vulkan"
    }

    companion object {

        @JvmStatic
        private val logger = logger<Vulkan>()
    }

}
