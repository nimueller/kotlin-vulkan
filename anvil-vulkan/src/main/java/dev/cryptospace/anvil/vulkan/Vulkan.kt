package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.AppConfig.useValidationLayers
import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.window.Window
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice.Companion.pickBestDevice
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.vulkan.VK10.vkDestroyInstance

object Vulkan : RenderingSystem() {

    @JvmStatic
    private val logger = logger<Vulkan>()

    override val renderingApi = RenderingApi.VULKAN
    private val validationLayers =
        VulkanValidationLayers(if (useValidationLayers) AppConfig.validationLayers else emptyList())
    val instance = VkInstanceFactory.createInstance(validationLayers)
    private val validationLayerLogger = VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }
    private val physicalDevices = PhysicalDevice.listPhysicalDevices(instance).also { physicalDevices ->
        logger.info("Found physical devices: $physicalDevices")
    }
    private val physicalDevice = physicalDevices.pickBestDevice().also { physicalDevice ->
        logger.info("Selected best physical device: $physicalDevice")
    }
    private val logicalDevice = LogicalDevice.create(physicalDevice)
    private val windowSurfaces = mutableMapOf<Window, VulkanSurface>()

    override fun initWindow(window: Window) {
        val surface = window.createSurface()
        windowSurfaces[window] = surface
        logger.info("Created Vulkan surface for window: $window")
    }

    override fun destroy() {
        physicalDevices.forEach { it.close() }
        logicalDevice.close()

        windowSurfaces.forEach { _, surface ->
            surface.close()
        }

        if (useValidationLayers) {
            validationLayerLogger.destroy()
        }

        vkDestroyInstance(instance, null)
    }

    override fun toString(): String {
        return "Vulkan"
    }

}
