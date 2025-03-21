package dev.cryptospace.anvil.core

import org.lwjgl.PointerBuffer

interface WindowSystem : AutoCloseable {

    var renderingApi: RenderingApi

    fun update()

    fun createWindow(): Window

    fun createSurface(window: Window, surface: PointerBuffer)

    fun getRequiredVulkanExtensions(): List<String>

}
