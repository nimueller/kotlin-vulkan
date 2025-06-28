package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

object SupportsRequiredQueuesCriteria : PhysicalDeviceSuitableCriteria {

    @JvmStatic
    private val logger = logger<SupportsRequiredExtensionsCriteria>()

    override fun isSuitable(device: PhysicalDevice): Boolean {
        val supportsGraphicsQueue = device.graphicsQueueFamilyIndex >= 0
        val supportsPresentQueue = device.presentQueueFamilyIndex >= 0
        logger.info("${device.name} supports graphics queue: $supportsGraphicsQueue")
        logger.info("${device.name} supports present queue: $supportsPresentQueue")
        return supportsGraphicsQueue && supportsPresentQueue
    }
}
