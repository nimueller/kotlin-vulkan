package dev.cryptospace.anvil.vulkan.device.suitable

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

fun interface PhysicalDeviceSuitableCriteria {

    fun isSuitable(device: PhysicalDevice): Boolean

    companion object {

        private val allCriteria = listOf(
            SupportsRequiredQueuesCriteria,
            SupportsRequiredExtensionsCriteria
        )

        fun allCriteriaSatisfied(device: PhysicalDevice): Boolean {
            return allCriteria.all { it.isSuitable(device) }
        }
    }
}
