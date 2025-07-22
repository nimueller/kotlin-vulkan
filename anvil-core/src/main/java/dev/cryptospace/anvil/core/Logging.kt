package dev.cryptospace.anvil.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates a SLF4J Logger instance for the specified type [T].
 *
 * This inline function provides a convenient way to create a type-specific logger
 * using reified type parameter, which allows accessing the class name at runtime.
 *
 * @return Logger instance configured for the specified type
 */
inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Extension function for SLF4J Logger that provides lazy message evaluation for debug level logging.
 *
 * The message is only evaluated if the debug log level is enabled, which can improve performance
 * when expensive message construction is involved.
 *
 * @param lazyMessage Lambda that produces the log message string
 */
fun Logger.debug(lazyMessage: () -> String) {
    if (isDebugEnabled) {
        debug(lazyMessage())
    }
}

/**
 * Extension function for SLF4J Logger that provides lazy message evaluation for warning level logging.
 *
 * The message is only evaluated if the warning log level is enabled, which can improve performance
 * when expensive message construction is involved.
 *
 * @param lazyMessage Lambda that produces the log message string
 */
fun Logger.warn(lazyMessage: () -> String) {
    if (isWarnEnabled) {
        warn(lazyMessage())
    }
}
