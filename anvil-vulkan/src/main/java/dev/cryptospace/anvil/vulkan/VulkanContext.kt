package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.pushStringList
import dev.cryptospace.anvil.core.putAllStrings
import dev.cryptospace.anvil.vulkan.utils.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.utils.VulkanValidationLayers
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.EXTDebugUtils
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

/**
 * Encapsulates the core Vulkan context, including instance and validation layers.
 *
 * This class manages the lifecycle of the Vulkan instance and related resources,
 * providing a centralized point of access for these core Vulkan components.
 * It handles initialization and cleanup of the Vulkan instance and validation layers.
 */
class VulkanContext(
    val handle: VkInstance,
) : NativeResource() {

    constructor(extensions: List<String>) : this(handle = createInstance(extensions))

    /**
     * Logger for Vulkan validation layer messages.
     * Only active if validation layers are enabled.
     */
    private val validationLayerLogger: VulkanValidationLayerLogger =
        VulkanValidationLayerLogger(handle).apply {
            if (AppConfig.useValidationLayers) {
                set()
            }
        }

    /**
     * Destroys the Vulkan instance and related resources.
     * This method should be called when the Vulkan context is no longer needed.
     */
    override fun destroy() {
        if (AppConfig.useValidationLayers) {
            validationLayerLogger.destroy()
        }

        VK10.vkDestroyInstance(handle, null)
    }

    companion object {

        /**
         * The Vulkan API version to use for this application.
         * Currently set to Vulkan 1.2 for enhanced features and performance.
         */
        val API_VERSION = VK12.VK_API_VERSION_1_2

        @JvmStatic
        private val logger = logger<VulkanContext>()

        /**
         * Manages Vulkan validation layers for debugging and error checking.
         * Initialized from [AppConfig.validationLayers] if provided, otherwise creates default validation layers.
         */
        private val validationLayers: VulkanValidationLayers = AppConfig.validationLayers.let { layers ->
            if (layers == null) {
                VulkanValidationLayers()
            } else {
                VulkanValidationLayers(layers)
            }
        }

        /**
         * Creates a new Vulkan instance with the specified GLFW and validation layer configuration.
         *
         * @param surfaceExtensions Surface extensions to use
         * @return Configured VkInstance ready for use
         */
        private fun createInstance(surfaceExtensions: List<String>): VkInstance = MemoryStack.stackPush().use { stack ->
            val appInfo = createApplicationInfo(stack)
            val createInfo = createInstanceCreateInfo(stack, surfaceExtensions, appInfo)
            val instance = createVulkanInstance(stack, createInfo)

            return VkInstance(instance[0], createInfo)
        }

        private fun createApplicationInfo(stack: MemoryStack): VkApplicationInfo =
            VkApplicationInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                pNext(MemoryUtil.NULL)
                pApplicationName(stack.ASCII("Hello Vulkan Application"))
                applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                pEngineName(stack.ASCII("Vulkan"))
                engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                apiVersion(API_VERSION)
            }

        private fun createInstanceCreateInfo(
            stack: MemoryStack,
            surfaceExtensions: List<String>,
            appInfo: VkApplicationInfo,
        ): VkInstanceCreateInfo {
            val additionalExtensions = getAdditionalVulkanExtensions()
            logger.info("Using extensions: ${surfaceExtensions + additionalExtensions}")

            val extensions = stack.callocPointer(surfaceExtensions.size + additionalExtensions.size)
            extensions.putAllStrings(stack, surfaceExtensions)
            extensions.putAllStrings(stack, additionalExtensions)
            extensions.flip()

            val layerNames = validationLayers.requestedLayerNames
            logger.info("Using validation layers: $layerNames")
            val layers = stack.pushStringList(layerNames)

            val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
                sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                pApplicationInfo(appInfo)
                ppEnabledLayerNames(layers)
                ppEnabledExtensionNames(extensions)
            }

            return createInfo
        }

        private fun getAdditionalVulkanExtensions(): List<String> {
            if (!AppConfig.useValidationLayers) {
                return emptyList()
            }

            return listOf(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
        }

        private fun createVulkanInstance(stack: MemoryStack, createInfo: VkInstanceCreateInfo): PointerBuffer {
            val pInstance = stack.callocPointer(1)
            VK10.vkCreateInstance(createInfo, null, pInstance).validateVulkanSuccess()
            return pInstance
        }
    }
}
