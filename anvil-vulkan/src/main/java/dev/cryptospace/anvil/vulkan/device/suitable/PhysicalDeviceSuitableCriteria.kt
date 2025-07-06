package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo

fun interface PhysicalDeviceSuitableCriteria {

    fun isSuitable(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): Boolean

    companion object {

        private val allCriteria = listOf(
            SupportsRequiredQueuesCriteria,
            SupportsRequiredExtensionsCriteria,
            SupportsAdequateSwapChain,
        )

        fun allCriteriaSatisfied(deviceSurfaceInfo: PhysicalDeviceSurfaceInfo): Boolean = allCriteria.all {
            it.isSuitable(deviceSurfaceInfo)
        }
    }
}
