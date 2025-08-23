package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanContext
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo.Companion.pickBestDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.surface.Surface

/**
 * Manages Vulkan physical and logical devices.
 *
 * This class handles the enumeration, selection, and management of physical devices,
 * as well as the creation and management of the logical device. It provides access
 * to the selected physical and logical devices for use by other components.
 *
 * @property vulkanContext The Vulkan context providing access to the Vulkan instance
 * @property surface The Vulkan surface for which device compatibility will be checked
 */
class DeviceManager(
    private val vulkanContext: VulkanContext,
    private val surface: Surface,
) : NativeResource() {

    /**
     * List of available physical devices (GPUs) that support Vulkan.
     */
    private val physicalDevices: List<PhysicalDevice> =
        PhysicalDevice.listPhysicalDevices(vulkanContext.handle).also {
            logger.info("Found ${it.size} physical devices")
        }

    /**
     * List of surface information details for each available physical device.
     * Contains capabilities, formats, and presentation modes supported by each device
     * when working with the current surface.
     */
    private val physicalDeviceSurfaceInfos: List<PhysicalDeviceSurfaceInfo> = physicalDevices.map { physicalDevice ->
        PhysicalDeviceSurfaceInfo(physicalDevice, surface)
    }

    /**
     * The selected physical device that best meets the application's requirements.
     * Chosen from available devices based on capabilities and performance characteristics.
     */
    private val selectedDeviceSurfaceInfo: PhysicalDeviceSurfaceInfo =
        physicalDeviceSurfaceInfos.pickBestDeviceSurfaceInfo().also { deviceSurfaceInfo ->
            logger.info("Selected best physical device: $deviceSurfaceInfo")
        }

    /**
     * The logical device providing the main interface for interacting with the physical device.
     * Created with specific queue families and features enabled for the application's needs.
     */
    val logicalDevice: LogicalDevice =
        LogicalDeviceFactory.create(selectedDeviceSurfaceInfo).also {
            logger.info("Created logical device: $it")
        }

    /**
     * Destroys the logical device and cleans up physical device resources.
     * This method should be called when the device manager is no longer needed.
     */
    override fun destroy() {
        logicalDevice.close()
        physicalDeviceSurfaceInfos.forEach { it.close() }
        physicalDevices.forEach { it.close() }
    }

    companion object {

        @JvmStatic
        private val logger = logger<DeviceManager>()
    }
}
