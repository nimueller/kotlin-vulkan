package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME

object SupportsRequiredExtensionsCriteria : PhysicalDeviceSuitableCriteria {

    val requiredExtensionNames = setOf(
        VK_KHR_SWAPCHAIN_EXTENSION_NAME,
    )

    @JvmStatic
    private val logger = logger<SupportsRequiredExtensionsCriteria>()

    override fun isSuitable(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): Boolean {
        val supportsRequiredExtensions = requiredExtensionNames.all { requiredExtensionName ->
            deviceSurfaceInfo.physicalDevice.extensions.any { availableExtension ->
                availableExtension.name == requiredExtensionName
            }
        }

        logger.info(
            "${deviceSurfaceInfo.physicalDevice.name} supports extensions: ${
                deviceSurfaceInfo.physicalDevice.extensions.map {
                    it.name
                }
            }",
        )
        logger.info("Required extensions: $requiredExtensionNames")
        logger.info(
            "${deviceSurfaceInfo.physicalDevice.name} supports required extensions: $supportsRequiredExtensions",
        )

        return supportsRequiredExtensions
    }
}
