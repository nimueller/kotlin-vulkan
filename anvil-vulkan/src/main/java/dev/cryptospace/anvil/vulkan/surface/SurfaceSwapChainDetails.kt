package dev.cryptospace.anvil.vulkan.surface

import dev.cryptospace.anvil.vulkan.device.PhysicalDevice

data class SurfaceSwapChainDetails(
    val physicalDevice: PhysicalDevice,
    val surfaceCapabilities: SurfaceCapabilities,
    val formats: List<SurfaceFormat>,
    val presentModes: List<SurfacePresentMode>,
)
