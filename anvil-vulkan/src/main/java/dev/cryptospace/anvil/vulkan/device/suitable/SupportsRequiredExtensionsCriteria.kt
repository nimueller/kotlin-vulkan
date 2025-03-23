package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME

object SupportsRequiredExtensionsCriteria : PhysicalDeviceSuitableCriteria {

    val requiredExtensionNames = setOf(
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    )

    @JvmStatic
    private val logger = logger<SupportsRequiredExtensionsCriteria>()

    override fun isSuitable(device: PhysicalDevice): Boolean {
        val supportsRequiredExtensions = requiredExtensionNames.all { requiredExtensionName ->
            device.extensions.any { availableExtension ->
                availableExtension.name == requiredExtensionName
            }
        }

        logger.info("${device.name} supports extensions: ${device.extensions.map { it.name }}")
        logger.info("Required extensions: $requiredExtensionNames")
        logger.info("${device.name} supports required extensions: $supportsRequiredExtensions")

        return supportsRequiredExtensions
    }
}
