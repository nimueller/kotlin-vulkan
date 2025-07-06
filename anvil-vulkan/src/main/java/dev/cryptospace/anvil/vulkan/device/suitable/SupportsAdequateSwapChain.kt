package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo

object SupportsAdequateSwapChain : PhysicalDeviceSuitableCriteria {

    @JvmStatic
    private val logger = logger<SupportsAdequateSwapChain>()

    override fun isSuitable(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): Boolean {
        val hasFormats = deviceSurfaceInfo.swapChainDetails.surfaceFormats.limit() > 0
        val hasPresentModes = deviceSurfaceInfo.swapChainDetails.surfacePresentModes.isNotEmpty()

        logger.info("Device supports formats: $hasFormats")
        logger.info("Device supports present modes: $hasPresentModes")

        return hasFormats && hasPresentModes
    }
}
