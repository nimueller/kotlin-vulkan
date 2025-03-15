package dev.cryptospace.vulkan

import org.slf4j.Logger

inline fun <reified T> getLogger(): Logger {
    return org.slf4j.LoggerFactory.getLogger(T::class.java)
}
