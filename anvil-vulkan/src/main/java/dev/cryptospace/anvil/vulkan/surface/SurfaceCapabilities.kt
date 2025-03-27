package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

data class SurfaceCapabilities(val device: PhysicalDevice, val surface: Surface)
