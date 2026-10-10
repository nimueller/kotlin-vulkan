package dev.cryptospace.anvil.core

object AppConfig {

    val preferX11 by lazy { exists("use-x11") }
    val useValidationLayers by lazy { exists("use-validation-layers") }
    val validationLayers by lazy { getArray("validation-layers") }

    /**
     * Closes the main loop after this many frames when set. Used for automated smoke runs of the examples.
     */
    val maxFrames by lazy { System.getProperty("max-frames")?.toLong() }

    private fun exists(key: String): Boolean = System.getProperty(key) != null

    private fun getArray(key: String): List<String>? = System.getProperty(key)?.split(",")
}
