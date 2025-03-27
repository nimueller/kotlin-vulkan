package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

object SupportsAdequateSwapChain : PhysicalDeviceSuitableCriteria {

    @JvmStatic
    private val logger = logger<SupportsAdequateSwapChain>()

    override fun isSuitable(device: PhysicalDevice): Boolean {
        val hasFormats = device.swapChainDetails.formats
        val hasPresentModes = device.swapChainDetails.presentModes

        logger.info("Device supports formats: $hasFormats")
        logger.info("Device supports present modes: $hasPresentModes")

        return hasFormats.isNotEmpty() && hasPresentModes.isNotEmpty()
    }
}
