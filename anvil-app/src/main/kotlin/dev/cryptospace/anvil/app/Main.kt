package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem

fun main() {
    Engine(
        renderingApi = RenderingApi.VULKAN,
        renderingSystemCreator = { glfw -> VulkanRenderingSystem(glfw) },
    ).use { engine ->
//        MainLoop(engine).loop()
    }
}
