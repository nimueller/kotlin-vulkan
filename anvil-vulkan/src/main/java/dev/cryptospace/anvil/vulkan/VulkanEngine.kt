package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingApi

class VulkanEngine :
    Engine(renderingApi = RenderingApi.VULKAN, { glfw ->
        VulkanRenderingSystem(glfw)
    })
