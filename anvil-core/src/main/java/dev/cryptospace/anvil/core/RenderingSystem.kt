package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.window.Window

abstract class RenderingSystem : NativeResource() {

    abstract val renderingApi: RenderingApi

    abstract fun initWindow(window: Window)

}
