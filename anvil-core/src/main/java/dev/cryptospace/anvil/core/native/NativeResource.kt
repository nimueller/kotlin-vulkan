package dev.cryptospace.anvil.core.native

import dev.cryptospace.anvil.core.logger

/**
 * Base class for resources that require explicit cleanup of native resources.
 * Implements [AutoCloseable] to ensure proper resource management and tracks destruction state.
 */
abstract class NativeResource : AutoCloseable {

    /**
     * Indicates if the resource is still alive and can be used.
     * @return true if the resource has not been destroyed, false otherwise
     */
    val isAlive: Boolean
        get() = !isDestroyed

    /**
     * Indicates if the resource has been destroyed.
     * Once destroyed, the resource cannot be used anymore.
     */
    var isDestroyed: Boolean = false
        private set

    /**
     * Closes and destroys the native resource.
     * If the resource is already destroyed, the operation is skipped.
     * Sets [isDestroyed] to true after cleanup.
     * @throws Throwable if resource destruction fails
     */
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

    /**
     * Performs the actual resource cleanup.
     * Must be implemented by subclasses to handle specific resource destruction.
     */
    protected abstract fun destroy()

    /**
     * Validates that the resource has not been destroyed.
     * @throws IllegalStateException if the resource is already destroyed
     */
    fun validateNotDestroyed() {
        check(isAlive) { "Resource is already destroyed" }
    }

    companion object {

        @JvmStatic
        private val logger = logger<NativeResource>()
    }
}
