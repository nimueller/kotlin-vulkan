package dev.cryptospace.anvil.glfw

import dev.cryptospace.anvil.core.RenderingApi
import dev.cryptospace.anvil.core.Window
import dev.cryptospace.anvil.core.WindowSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.toStringList
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwTerminate
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions

object GlfwWindowSystem : WindowSystem {

    @JvmStatic
    private val logger = logger<GlfwWindowSystem>()

    override var renderingApi: RenderingApi = RenderingApi.OPENGL

    init {
        GlfwInitProcess.init()
    }

    override fun update() {
        glfwPollEvents()
    }

    override fun createWindow(): Window {
        if (renderingApi != RenderingApi.VULKAN) {
            logger.error("Currently only Vulkan rendering system is supported, trying nonetheless")
        }

        return GlfwWindowFactory.createWindow(renderingApi)
    }

    override fun createSurface(window: Window, surface: PointerBuffer) {
        TODO("Not yet implemented")
    }

    override fun getRequiredVulkanExtensions(): List<String> {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()
        checkNotNull(glfwExtensions) { "Failed to find list of required Vulkan extensions" }
        val extensionNames = glfwExtensions.toStringList()
        logger.info("Found required Vulkan extensions: $extensionNames")
        return extensionNames
    }

    override fun close() {
        glfwTerminate()
    }

}
