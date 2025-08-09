package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.vulkan.graphics.SwapChain

class VulkanRenderingContext(
    val engine: Engine,
    private val swapChain: SwapChain,
) : RenderingContext {

    override val width: Int
        get() = swapChain.extent.width()

    override val height: Int
        get() = swapChain.extent.height()
}
