package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.MainLoop
import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem

fun main() {
    Engine(
        renderingApi = RenderingApi.VULKAN,
        renderingSystemCreator = { glfw -> VulkanRenderingSystem(glfw) },
//        renderingApi = RenderingApi.OPENGL,
//        renderingSystemCreator = { glfw -> OpenGLRenderingSystem(glfw) },
    ).use { engine ->
        MainLoop(engine).loop { glfw, _ ->
            if (glfw.isKeyPressed(Key.ESCAPE)) {
                glfw.window.requestClose()
            }
        }
    }
}
