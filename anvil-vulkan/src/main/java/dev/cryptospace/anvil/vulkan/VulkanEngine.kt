package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.RenderingApi

class VulkanEngine :
    Engine(renderingApi = RenderingApi.VULKAN, { glfw ->
        VulkanRenderingSystem(glfw)
    }) {
    companion object {
        init {
            // Increase LWJGL MemoryStack size to avoid OutOfMemoryError: Out of stack space
            // on systems with many Vulkan layers or extensions.
            if (System.getProperty("org.lwjgl.system.stackSize") == null) {
                System.setProperty("org.lwjgl.system.stackSize", "256") // 256 KB
            }
        }
    }
}

fun vulkan(block: VulkanEngine.() -> Unit): Unit = VulkanEngine().use { engine ->
    engine.block()
    MainLoop(engine).loop()
}
