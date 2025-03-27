package dev.cryptospace.anvil.core.native

import dev.cryptospace.anvil.core.logger

abstract class NativeResource : AutoCloseable {

    val isAlive: Boolean
        get() = !isDestroyed
    var isDestroyed: Boolean = false
        private set

    override fun close() {
        if (isDestroyed) {
            logger.debug("Already destroyed, skipping")
            return
        }

        try {
            logger.debug("Destroying {}", this)
            destroy()
            logger.info("Destroyed $this")
        } catch (e: Throwable) {
            logger.error("Failed to destroy $this", e)
            throw e
        } finally {
            isDestroyed = true
        }
    }

    protected abstract fun destroy()

    fun validateNotDestroyed() {
        check(isAlive) { "Resource is already destroyed" }
    }

    companion object {

        @JvmStatic
        private val logger = logger<NativeResource>()
    }
}
