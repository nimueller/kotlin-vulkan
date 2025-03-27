package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.surface.Surface

fun interface PhysicalDeviceSuitableCriteria {

    fun isSuitable(device: PhysicalDevice): Boolean

    companion object {

        private val allCriteria = listOf(
            SupportsRequiredQueuesCriteria,
            SupportsRequiredExtensionsCriteria,
            SupportsAdequateSwapChain
        )

        fun allCriteriaSatisfied(device: PhysicalDevice, surface: Surface): Boolean {
            return allCriteria.all { it.isSuitable(device) }
        }
    }
}
