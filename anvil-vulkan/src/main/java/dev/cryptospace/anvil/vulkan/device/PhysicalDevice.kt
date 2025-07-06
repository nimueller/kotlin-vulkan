package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanRenderingSystem
import dev.cryptospace.anvil.vulkan.device.suitable.PhysicalDeviceSuitableCriteria
import dev.cryptospace.anvil.vulkan.queryVulkanPointerBuffer
import dev.cryptospace.anvil.vulkan.surface.Surface
import dev.cryptospace.anvil.vulkan.surface.SurfaceSwapChainDetails
import dev.cryptospace.anvil.vulkan.transform
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkQueueFamilyProperties
import java.nio.ByteBuffer

data class PhysicalDevice(
    val vulkan: VulkanRenderingSystem,
    val handle: VkPhysicalDevice,
) : NativeResource() {

    val properties: VkPhysicalDeviceProperties =
        VkPhysicalDeviceProperties.malloc().apply {
            vkGetPhysicalDeviceProperties(handle, this)
        }

    val features: VkPhysicalDeviceFeatures =
        VkPhysicalDeviceFeatures.malloc().apply {
            vkGetPhysicalDeviceFeatures(handle, this)
        }

    val queueFamilies: VkQueueFamilyProperties.Buffer =
        MemoryStack.stackPush().use { stack ->
            val countBuffer = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, null)

            val queueBuffer = VkQueueFamilyProperties.malloc(countBuffer[0])
            vkGetPhysicalDeviceQueueFamilyProperties(handle, countBuffer, queueBuffer)

            queueBuffer
        }

    val graphicsQueueFamilyIndex =
        queueFamilies.indexOfFirst { it.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0 }

    val extensions =
        MemoryStack.stackPush().use { stack ->
            val extensionCount = stack.mallocInt(1)
            val layerName: ByteBuffer? = null
            vkEnumerateDeviceExtensionProperties(handle, layerName, extensionCount, null)

            val availableExtensions = VkExtensionProperties.malloc(extensionCount[0], stack)
            vkEnumerateDeviceExtensionProperties(handle, layerName, extensionCount, availableExtensions)

            availableExtensions.map { DeviceExtension(it.extensionNameString()) }
        }

    val name: String = properties.deviceNameString()

    var presentQueueFamilyIndex = -1
        private set
    lateinit var swapChainDetails: SurfaceSwapChainDetails
        private set

    fun initSurface(surface: Surface) {
        this.validateNotDestroyed()
        surface.validateNotDestroyed()

        refreshPresentQueueFamily(surface)
        swapChainDetails = SurfaceSwapChainDetails(this, surface)
    }

    private fun refreshPresentQueueFamily(surface: Surface) {
        presentQueueFamilyIndex = -1

        MemoryStack.stackPush().use { stack ->
            val presentSupport = stack.mallocInt(1)

            queueFamilies.forEachIndexed { index, _ ->
                val result = vkGetPhysicalDeviceSurfaceSupportKHR(
                    handle,
                    index,
                    surface.address.handle,
                    presentSupport,
                )
                check(result == VK_SUCCESS) { "Failed to query for surface support capabilities" }

                if (presentSupport[0] != VK_FALSE) {
                    presentQueueFamilyIndex = index
                    return
                }
            }
        }

        error("Failed to find a suitable queue family which supported presentation queue")
    }

    override fun toString(): String = "${this::class.simpleName}(name=$name)"

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
        properties.free()
        features.free()
        queueFamilies.free()
        swapChainDetails.close()
    }

    companion object {

        fun List<PhysicalDevice>.pickBestDevice(surface: Surface): PhysicalDevice {
            val selectedDevice = firstOrNull { device ->
                PhysicalDeviceSuitableCriteria.allCriteriaSatisfied(device, surface)
            }
            checkNotNull(selectedDevice) { "Failed to find a suitable GPU" }
            return selectedDevice
        }

        fun listPhysicalDevices(vulkan: VulkanRenderingSystem): List<PhysicalDevice> {
            val instance = vulkan.instance
            return MemoryStack.stackPush().use { stack ->
                stack.queryVulkanPointerBuffer { countBuffer, pointerBuffer ->
                    vkEnumeratePhysicalDevices(instance, countBuffer, pointerBuffer)
                }.transform { address ->
                    val handle = VkPhysicalDevice(address, instance)
                    PhysicalDevice(vulkan, handle)
                }
            }
        }
    }
}
