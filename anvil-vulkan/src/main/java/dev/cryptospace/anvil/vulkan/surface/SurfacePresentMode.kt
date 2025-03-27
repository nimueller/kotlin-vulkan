package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

data class SurfacePresentMode(val device: PhysicalDevice, val surface: Surface)
