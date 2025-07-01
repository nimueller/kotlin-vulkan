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
