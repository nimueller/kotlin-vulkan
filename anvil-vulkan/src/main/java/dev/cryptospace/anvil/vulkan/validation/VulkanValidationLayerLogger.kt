package dev.cryptospace.anvil.vulkan.validation

import dev.cryptospace.anvil.core.logger
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT
import org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT
import org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXTI
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkInstance

const val SEVERITY = VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
    .or(VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT)
    .or(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
    .or(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
const val TYPE = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
    .or(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT)
    .or(VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)

class VulkanValidationLayerLogger(
    private val vulkanInstance: VkInstance,
) : VkDebugUtilsMessengerCallbackEXTI {

    private var currentMessengerHandle: Long? = 0

    fun set() = MemoryStack.stackPush().use { stack ->
        val createInfo = VkDebugUtilsMessengerCreateInfoEXT.create().apply {
            sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
            messageSeverity(SEVERITY)
            messageType(TYPE)
            pfnUserCallback(this@VulkanValidationLayerLogger)
            pUserData(NULL)
        }

        val debugMessenger = stack.mallocLong(1)
        val result = vkCreateDebugUtilsMessengerEXT(vulkanInstance, createInfo, null, debugMessenger)
        check(result == VK_SUCCESS) { "Failed to set up debug messenger" }
        currentMessengerHandle = debugMessenger[0]

        logger.info("Vulkan validation layer logging set up")
        this
    }

    fun destroy() {
        val handle = currentMessengerHandle

        if (handle == null) {
            logger.warn("Vulkan validation layer logger was not set up, skipping destruction")
            return
        }

        vkDestroyDebugUtilsMessengerEXT(vulkanInstance, handle, null)
        logger.info("Vulkan validation layer logging destroyed")
    }

    override fun invoke(messageSeverity: Int, messageTypes: Int, pCallbackData: Long, pUserData: Long): Int {
        val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
        val message = callbackData.pMessageString()

        when (messageSeverity) {
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> logger.debug(message)
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> logger.info(message)
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT -> logger.warn(message, IllegalStateException(message))
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> logger.error(message, IllegalStateException(message))
            else -> logger.info(message)
        }

        return VK_FALSE
    }

    companion object {

        @JvmStatic
        private val logger = logger<VulkanValidationLayerLogger>()
    }
}
