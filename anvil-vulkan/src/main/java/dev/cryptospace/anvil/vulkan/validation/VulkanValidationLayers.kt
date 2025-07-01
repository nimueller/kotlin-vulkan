package dev.cryptospace.anvil.vulkan.validation

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.vulkan.queryAndTransformVulkanStructBuffer
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties
import org.lwjgl.vulkan.VkLayerProperties

/**
 * Manages Vulkan validation layers functionality and validation.
 *
 * Validation layers provide debug functionality to detect API misuse, track resource management,
 * and verify various Vulkan operations. This class ensures that requested validation layers
 * are available in the current Vulkan installation before enabling them.
 */
class VulkanValidationLayers(val requestedLayerNames: List<String> = listOf(KHRONOS_VALIDATION)) {

    companion object {

        /**
         * Standard Vulkan validation layer name provided by Khronos Group.
         *
         * This validation layer combines multiple validation features into a single layer,
         * providing comprehensive validation and debugging capabilities for Vulkan applications.
         * It checks for API usage errors, resource leaks, and synchronization issues.
         */
        const val KHRONOS_VALIDATION = "VK_LAYER_KHRONOS_validation"

        @JvmStatic
        private val logger = logger<VulkanValidationLayers>()
    }

    /**
     * Names of validation layers supported by the current Vulkan installation.
     *
     * Retrieved by querying the Vulkan instance for available layer properties
     * using vkEnumerateInstanceLayerProperties. Each layer name is extracted
     * from the returned VkLayerProperties buffer.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val supportedLayerNames: List<String> =
        queryAndTransformVulkanStructBuffer(
            bufferInitializer = { VkLayerProperties.malloc(it, this) },
            bufferQuery = { countBuffer, resultBuffer ->
                vkEnumerateInstanceLayerProperties(countBuffer, resultBuffer)
            },
            bufferMapper = { it.layerNameString() },
        ).also { supportedLayerNames ->
            logger.info("Following validation layers are supported: $supportedLayerNames")
        }

    init {
        validateRequestedLayersSupported(requestedLayerNames)
        logger.info("Vulkan validation layers initialized")
    }

    /**
     * Validates that all requested validation layers are supported.
     *
     * @param requestedLayerNames List of validation layer names to check
     * @throws IllegalStateException if any requested layer is not supported
     */
    private fun validateRequestedLayersSupported(requestedLayerNames: List<String>) {
        val unsupportedLayers = getUnavailableLayers(requestedLayerNames)
        check(unsupportedLayers.isEmpty()) { "Requested unsupported validation layers: $unsupportedLayers" }
    }

    /**
     * Returns a list of requested layers that are not available.
     *
     * @param requestedLayerNames List of validation layer names to check
     * @return List of layer names that are not supported
     */
    private fun getUnavailableLayers(requestedLayerNames: List<String>): List<String> =
        requestedLayerNames.filter { !isLayerSupported(it) }

    /**
     * Checks if a specific validation layer is supported.
     *
     * @param layerName Name of the validation layer to check
     * @return true if the layer is supported, false otherwise
     */
    private fun isLayerSupported(layerName: String): Boolean = supportedLayerNames.contains(layerName)
}
