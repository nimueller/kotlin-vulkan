package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.suitable.PhysicalDeviceSuitableCriteria
import dev.cryptospace.anvil.vulkan.surface.Surface
import dev.cryptospace.anvil.vulkan.surface.SurfaceSwapChainDetails
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_SUCCESS

/**
 * Represents the relationship between a physical device (GPU) and a surface in Vulkan.
 * This class holds information about surface capabilities, presentation support,
 * and swap chain details for a specific physical device and surface combination.
 *
 * @property physicalDevice The physical device (GPU) to be queried for surface support
 * @property surface The Vulkan surface to be used for presentation
 */
data class PhysicalDeviceSurfaceInfo(
    val physicalDevice: PhysicalDevice,
    val surface: Surface,
) : NativeResource() {

    init {
        physicalDevice.validateNotDestroyed()
        surface.validateNotDestroyed()
    }

    /**
     * The queue family index that supports presentation operations for this surface.
     * This index is determined by querying each queue family of the physical device
     * for presentation support with the given surface.
     */
    val presentQueueFamilyIndex: Int =
        MemoryStack.stackPush().use { stack ->
            val presentSupport = stack.mallocInt(1)

            physicalDevice.queueFamilies.forEachIndexed { index, _ ->
                val result = vkGetPhysicalDeviceSurfaceSupportKHR(
                    physicalDevice.handle,
                    index,
                    surface.handle.value,
                    presentSupport,
                )
                check(result == VK_SUCCESS) { "Failed to query for surface support capabilities" }

                if (presentSupport[0] != VK_FALSE) {
                    return@use index
                }
            }

            error("Failed to find a suitable queue family which supported presentation queue")
        }

    /**
     * Details about the swap chain capabilities and properties for this physical device and surface combination.
     * This includes information about surface formats, present modes, and other swap chain related capabilities.
     * The property can only be modified internally, but can be refreshed using [refreshSwapChainDetails].
     */
    var swapChainDetails: SurfaceSwapChainDetails = SurfaceSwapChainDetails.query(this)
        private set

    /**
     * Refreshes the swap chain details by closing the existing details and querying for new ones.
     * This is useful when the surface properties might have changed, such as after a window resize.
     *
     * @return The newly queried [SurfaceSwapChainDetails]
     */
    fun refreshSwapChainDetails(): SurfaceSwapChainDetails {
        swapChainDetails.close()
        swapChainDetails = SurfaceSwapChainDetails.query(this)
        return swapChainDetails
    }

    override fun destroy() {
        swapChainDetails.close()
    }

    companion object {

        /**
         * Extension function to select the most suitable physical device and surface combination
         * from a list of device-surface pairs based on defined suitability criteria.
         *
         * @throws IllegalStateException if no suitable device is found
         * @return The most suitable [PhysicalDeviceSurfaceInfo] based on the criteria
         */
        fun List<PhysicalDeviceSurfaceInfo>.pickBestDeviceSurfaceInfo(): PhysicalDeviceSurfaceInfo {
            val selectedDevice = firstOrNull { deviceSurfaceInfo ->
                PhysicalDeviceSuitableCriteria.allCriteriaSatisfied(deviceSurfaceInfo)
            }
            checkNotNull(selectedDevice) { "Failed to find a suitable GPU" }
            return selectedDevice
        }
    }
}
