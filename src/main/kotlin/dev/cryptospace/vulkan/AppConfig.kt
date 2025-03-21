package dev.cryptospace.vulkan

import dev.cryptospace.vulkan.core.VK_KHRONOS_VALIDATION_LAYER_NAME

object AppConfig {

    val preferX11 by lazy { exists("use-x11") }
    val useValidationLayers by lazy { exists("use-validation-layers") }
    val validationLayers by lazy { getArray("validation-layers", VK_KHRONOS_VALIDATION_LAYER_NAME) }

    private fun exists(key: String): Boolean {
        return System.getProperty(key) != null
    }

    private fun getArray(key: String, vararg default: String): List<String> {
        return System.getProperty(key)?.split(",") ?: default.toList()
    }

}
