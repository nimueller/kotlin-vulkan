package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.vulkan.Vulkan

fun main() {
    Engine(
        renderingSystemCreator = { Vulkan }
    ).use { engine ->
//        MainLoop(engine).loop()
    }
}
