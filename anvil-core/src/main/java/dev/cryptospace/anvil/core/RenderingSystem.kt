package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.native.NativeResource

abstract class RenderingSystem : NativeResource() {

    abstract fun drawFrame()
}
