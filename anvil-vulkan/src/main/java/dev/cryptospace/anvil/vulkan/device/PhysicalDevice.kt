package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.queryVulkanBufferWithoutSuccessValidation
import dev.cryptospace.anvil.vulkan.queryVulkanPointerBuffer
import dev.cryptospace.anvil.vulkan.transform
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties
import java.nio.ByteBuffer

/**
 * Represents a physical device (GPU) in the Vulkan graphics system.
 * Manages device-specific properties, features, and capabilities.
 * Acts as a wrapper around the native Vulkan physical device handle.
 * @param handle The native Vulkan physical device handle
 */
data class PhysicalDevice(
    val handle: VkPhysicalDevice,
) : NativeResource() {

    /** Physical device properties including device limits and capabilities */
    val properties: VkPhysicalDeviceProperties =
        VkPhysicalDeviceProperties.malloc().apply {
            vkGetPhysicalDeviceProperties(handle, this)
        }

    /** Optional features supported by the physical device */
    val features: VkPhysicalDeviceFeatures =
        VkPhysicalDeviceFeatures.malloc().apply {
            vkGetPhysicalDeviceFeatures(handle, this)
        }

    /** Queue families supported by this physical device */
    val queueFamilies: VkQueueFamilyProperties.Buffer =
        MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBufferWithoutSuccessValidation(
                bufferInitializer = { VkQueueFamilyProperties.malloc(it) },
                bufferQuery = { countBuffer, resultBuffer ->
                    vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, resultBuffer)
                },
            )
        }

    /** Index of the queue family that supports graphics operations */
    val graphicsQueueFamilyIndex =
        queueFamilies.indexOfFirst { it.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0 }

    /** List of supported device extensions */
    val extensions =
        MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBuffer(
                bufferInitializer = { VkExtensionProperties.malloc(it) },
                bufferQuery = { countBuffer, resultBuffer ->
                    vkEnumerateDeviceExtensionProperties(handle, null as ByteBuffer?, countBuffer, resultBuffer)
                },
            ).transform { extensionProperties ->
                DeviceExtension(extensionProperties.extensionNameString())
            }
        }

    /** The name of the physical device */
    val name: String = properties.deviceNameString()

    override fun toString(): String = "${this::class.simpleName}(name=$name)"

    override fun destroy() {
        properties.free()
        features.free()
        queueFamilies.free()
    }

    companion object {

        @JvmStatic
        private val logger = logger<PhysicalDevice>()

        /**
         * Lists all available physical devices (GPUs) in the system.
         * @param vulkanInstance The vulkan instance
         * @return List of available physical devices
         */
        fun listPhysicalDevices(vulkanInstance: VkInstance): List<PhysicalDevice> =
            MemoryStack.stackPush().use { stack ->
                stack.queryVulkanPointerBuffer { countBuffer, pointerBuffer ->
                    vkEnumeratePhysicalDevices(vulkanInstance, countBuffer, pointerBuffer)
                }.transform { address ->
                    val handle = VkPhysicalDevice(address, vulkanInstance)
                    PhysicalDevice(handle)
                }.also { physicalDevices ->
                    logger.info("Found physical devices: $physicalDevices")
                }
            }
    }
}
