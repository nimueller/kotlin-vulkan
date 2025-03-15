package dev.cryptospace.vulkan

import dev.cryptospace.vulkan.core.VulkanValidationLayers
import dev.cryptospace.vulkan.utils.getLogger
import dev.cryptospace.vulkan.utils.pushStringList
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

const val VK_KHRONOS_VALIDATION_LAYER_NAME = "VK_LAYER_KHRONOS_validation"

class Vulkan(
    private val useValidationLayers: Boolean,
    private val validationLayerNames: List<String>
) : AutoCloseable {

    val instance: VkInstance = createInstance()
    val validationLayers: VulkanValidationLayers =
        VulkanValidationLayers(if (useValidationLayers) validationLayerNames else emptyList())

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

        logger.info("Using validation layers: $validationLayerNames")
        val activatedLayers = pushStringList(validationLayerNames)

        val createInfo = VkInstanceCreateInfo.malloc(this).apply {
            sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            pApplicationInfo(appInfo)
            ppEnabledLayerNames(activatedLayers)
            ppEnabledExtensionNames(glfwExtensions)
        }

        return createInfo
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
