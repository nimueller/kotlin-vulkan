package dev.cryptospace.anvil.vulkan.utils

import org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT
import org.lwjgl.vulkan.VK10.VK_ERROR_DEVICE_LOST
import org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_FEATURE_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_FORMAT_NOT_SUPPORTED
import org.lwjgl.vulkan.VK10.VK_ERROR_INCOMPATIBLE_DRIVER
import org.lwjgl.vulkan.VK10.VK_ERROR_INITIALIZATION_FAILED
import org.lwjgl.vulkan.VK10.VK_ERROR_LAYER_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_MEMORY_MAP_FAILED
import org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY
import org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_HOST_MEMORY
import org.lwjgl.vulkan.VK10.VK_ERROR_TOO_MANY_OBJECTS

/**
 * Base exception class for Vulkan-related errors.
 *
 * This class serves as the parent for all Vulkan-specific exceptions,
 * providing common functionality for error reporting and handling.
 *
 * @property resultCode The Vulkan result code that caused the exception
 * @property operation Description of the operation that failed (optional)
 */
open class VulkanException(
    private val resultCode: Int,
    private val operation: String? = null,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
    buildMessage(resultCode, operation, message),
    cause,
) {

    companion object {

        /**
         * Builds a descriptive error message from the provided parameters.
         *
         * @param resultCode The Vulkan result code
         * @param operation Description of the operation that failed (optional)
         * @param message Additional error message (optional)
         * @return A formatted error message
         */
        private fun buildMessage(resultCode: Int, operation: String?, message: String?): String {
            val resultName = vulkanResultDisplayNameMap[resultCode] ?: resultCode.toString()
            val operationText = operation?.let { "Operation '$it' failed: " } ?: ""
            val messageText = message?.let { ": $it" } ?: ""

            return "${operationText}Vulkan error $resultName$messageText"
        }
    }
}

/**
 * Exception thrown when a Vulkan operation fails due to memory allocation issues.
 */
class VulkanMemoryException(
    resultCode: Int,
    operation: String? = null,
    message: String? = null,
) : VulkanException(resultCode, operation, message)

/**
 * Exception thrown when a Vulkan operation fails due to device issues.
 */
class VulkanDeviceException(
    resultCode: Int,
    operation: String? = null,
    message: String? = null,
) : VulkanException(resultCode, operation, message)

/**
 * Exception thrown when a Vulkan operation fails due to initialization issues.
 */
class VulkanInitializationException(
    resultCode: Int,
    operation: String? = null,
    message: String? = null,
) : VulkanException(resultCode, operation, message)

/**
 * Exception thrown when a Vulkan operation fails due to validation errors.
 */
class VulkanValidationException(
    resultCode: Int,
    operation: String? = null,
    message: String? = null,
) : VulkanException(resultCode, operation, message)

/**
 * Exception thrown when a Vulkan operation fails for an unknown or unexpected reason.
 */
class VulkanUnknownException(
    resultCode: Int,
    operation: String? = null,
    message: String? = null,
) : VulkanException(resultCode, operation, message)

/**
 * Creates the appropriate VulkanException subclass based on the result code.
 *
 * @param resultCode The Vulkan result code
 * @param operation Description of the operation that failed (optional)
 * @param message Additional error message (optional)
 * @return A VulkanException instance of the appropriate subclass
 */
fun createVulkanException(resultCode: Int, operation: String? = null, message: String? = null): VulkanException =
    when (resultCode) {
        VK_ERROR_OUT_OF_HOST_MEMORY,
        VK_ERROR_OUT_OF_DEVICE_MEMORY,
        VK_ERROR_MEMORY_MAP_FAILED,
        VK_ERROR_TOO_MANY_OBJECTS,
        -> VulkanMemoryException(resultCode, operation, message)

        VK_ERROR_DEVICE_LOST,
        VK_ERROR_INCOMPATIBLE_DRIVER,
        VK_ERROR_FEATURE_NOT_PRESENT,
        VK_ERROR_EXTENSION_NOT_PRESENT,
        VK_ERROR_FORMAT_NOT_SUPPORTED,
        -> VulkanDeviceException(resultCode, operation, message)

        VK_ERROR_INITIALIZATION_FAILED,
        VK_ERROR_LAYER_NOT_PRESENT,
        -> VulkanInitializationException(resultCode, operation, message)

        VK_ERROR_VALIDATION_FAILED_EXT,
        -> VulkanValidationException(resultCode, operation, message)

        else
        -> VulkanUnknownException(resultCode, operation, message)
    }
