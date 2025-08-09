package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.device.LogicalDevice

class VulkanRenderingContext(
    val engine: Engine,
    private val logicalDevice: LogicalDevice,
) : RenderingContext {

    override val width: Int
        get() = logicalDevice.swapChain.extent.width()

    override val height: Int
        get() = logicalDevice.swapChain.extent.height()
}
