package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo

object SupportsRequiredQueuesCriteria : PhysicalDeviceSuitableCriteria {

    @JvmStatic
    private val logger = logger<SupportsRequiredExtensionsCriteria>()

    override fun isSuitable(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): Boolean {
        val supportsGraphicsQueue = deviceSurfaceInfo.physicalDevice.graphicsQueueFamilyIndex >= 0
        val supportsPresentQueue = deviceSurfaceInfo.presentQueueFamilyIndex >= 0
        logger.info("${deviceSurfaceInfo.physicalDevice.name} supports graphics queue: $supportsGraphicsQueue")
        logger.info("${deviceSurfaceInfo.physicalDevice.name} supports present queue: $supportsPresentQueue")
        return supportsGraphicsQueue && supportsPresentQueue
    }
}
