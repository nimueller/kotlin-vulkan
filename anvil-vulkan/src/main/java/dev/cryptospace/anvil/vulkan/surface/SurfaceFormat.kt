package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

data class SurfaceFormat(val device: PhysicalDevice, val surface: Surface)
