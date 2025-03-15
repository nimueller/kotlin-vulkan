package dev.cryptospace.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkLayerProperties

const val VK_KHRONOS_VALIDATION_LAYER_NAME = "VK_LAYER_KHRONOS_validation"

class Vulkan(
    private val useValidationLayers: Boolean,
    private val validationLayers: List<String>
) : AutoCloseable {

    val instance: VkInstance = createInstance()

    private fun createInstance(): VkInstance = MemoryStack.stackPush().use { stack ->
        val appInfo = stack.createApplicationInfo()
        val createInfo = stack.createInstanceCreateInfo(appInfo)
        val instance = stack.createVulkanInstance(createInfo)

        return VkInstance(instance[0], createInfo)
    }

    private fun MemoryStack.createApplicationInfo(): VkApplicationInfo {
        val appInfo = VkApplicationInfo.malloc(this).apply {
            sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            pApplicationName(ASCII("Hello Vulkan Application"))
            applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            pEngineName(ASCII("Vulkan"))
            engineVersion(VK_MAKE_VERSION(1, 0, 0))
            apiVersion(VK_MAKE_VERSION(1, 0, 0))
        }
        return appInfo
    }

    private fun MemoryStack.createInstanceCreateInfo(appInfo: VkApplicationInfo): VkInstanceCreateInfo {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()
        checkNotNull(glfwExtensions) { "Unable to get GLFW Vulkan extensions" }

        val validationLayers = if (useValidationLayers) {
            logger.info("Using validation layers: $validationLayers")
            val layers = mallocPointer(validationLayers.size)
            validationLayers.forEachIndexed { index, layer ->
                layers.put(index, ASCII(layer))
            }
            layers.flip()
            validateLayersSupported(layers)
            layers
        } else {
            logger.info("Not using validation layers")
            mallocPointer(0)
        }

        val createInfo = VkInstanceCreateInfo.malloc(this).apply {
            sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            pApplicationInfo(appInfo)
            ppEnabledLayerNames(validationLayers)
            ppEnabledExtensionNames(glfwExtensions)
        }

        return createInfo
    }

    private fun MemoryStack.validateLayersSupported(requestedLayers: PointerBuffer) {
        val supportedLayerNames = getSupportedLayerNames().toList().map { it.layerNameString() }
        val requestButNotSupported = mutableListOf<String>()

        while (requestedLayers.hasRemaining()) {
            val requestedLayerName = requestedLayers.stringASCII
            val layerFound = supportedLayerNames.any { it == requestedLayerName }

            if (!layerFound) {
                requestButNotSupported.add(requestedLayerName)
            }
        }

        logger.info("Following validation layers are supported: $supportedLayerNames")
        check(requestButNotSupported.isEmpty()) { "Requested unsupported validation layers: $requestButNotSupported" }
        requestedLayers.rewind()
    }

    private fun MemoryStack.getSupportedLayerNames(): VkLayerProperties.Buffer {
        val layerCount = mallocInt(1)
        var result = vkEnumerateInstanceLayerProperties(layerCount, null)
        check(result == VK_SUCCESS) { "Failed to enumerate Vulkan instance layer properties: $result" }

        if (layerCount[0] == 0) {
            logger.warn("Found no supported Vulkan instance layers")
            return VkLayerProperties.malloc(0)
        }

        val supportedLayerNames = VkLayerProperties.malloc(layerCount[0])
        result = vkEnumerateInstanceLayerProperties(layerCount, supportedLayerNames)
        check(result == VK_SUCCESS) { "Failed to enumerate Vulkan instance layer properties: $result" }
        return supportedLayerNames
    }

    private fun MemoryStack.createVulkanInstance(createInfo: VkInstanceCreateInfo): PointerBuffer {
        val instance = mallocPointer(1)
        val result = vkCreateInstance(createInfo, null, instance)
        check(result == VK_SUCCESS) { "Failed to create Vulkan instance: $result" }
        return instance
    }

    override fun close() {
        vkDestroyInstance(instance, null)
    }

    companion object {
        @JvmStatic
        private val logger = getLogger<Vulkan>()
    }

}
