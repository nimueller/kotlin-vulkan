package dev.cryptospace.anvil.core

const val VK_KHRONOS_VALIDATION_LAYER_NAME = "VK_LAYER_KHRONOS_validation"

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
