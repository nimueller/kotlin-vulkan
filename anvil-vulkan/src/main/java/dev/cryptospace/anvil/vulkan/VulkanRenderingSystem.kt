package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.AppConfig.useValidationLayers
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.device.LogicalDeviceFactory
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo.Companion.pickBestDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.device.SwapChain
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.vulkan.VK10.vkDestroyInstance

/**
 * Main Vulkan rendering system implementation that manages the Vulkan graphics API lifecycle.
 *
 * This class handles initialization and cleanup of core Vulkan components.
 *
 * @property glfw GLFW window system integration for surface creation
 * @constructor Creates a new Vulkan rendering system with the specified GLFW instance
 */
class VulkanRenderingSystem(
    glfw: Glfw,
) : RenderingSystem() {

    /**
     * Manages Vulkan validation layers for debugging and error checking.
     * Initialized from [AppConfig.validationLayers] if provided, otherwise creates default validation layers.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val validationLayers =
        AppConfig.validationLayers.let { layers ->
            if (layers == null) {
                VulkanValidationLayers()
            } else {
                VulkanValidationLayers(layers)
            }
        }

    /**
     * The main Vulkan instance handle created by [VkInstanceFactory].
     *
     * This instance serves as the primary connection point to the Vulkan API and is required
     * for creating other Vulkan objects. It is configured with the specified validation layers
     * and GLFW integration.
     */
    val instance =
        VkInstanceFactory.createInstance(glfw, validationLayers)

    private val validationLayerLogger =
        VulkanValidationLayerLogger(instance).apply { if (useValidationLayers) set() }

    /**
     * List of available physical devices (GPUs) that support Vulkan.
     * Note: At this stage, physical devices don't have any surface-related information.
     * Surface capabilities, formats, and presentation modes for each device are stored
     * in the [physicalDeviceSurfaceInfos] property once the surface was created.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val physicalDevices =
        PhysicalDevice.listPhysicalDevices(this).also { physicalDevices ->
            logger.info("Found physical devices: $physicalDevices")
        }

    /**
     * The Vulkan surface used for rendering to the window.
     * Created from the GLFW window and provides the interface between Vulkan and the window system.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val surface =
        glfw.window.createSurface(this).also { surface ->
            logger.info("Created surface: $surface")
        }

    /**
     * List of surface information details for each available physical device.
     * Contains capabilities, formats, and presentation modes supported by each device
     * when working with the current surface.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val physicalDeviceSurfaceInfos =
        physicalDevices.map { physicalDevice ->
            PhysicalDeviceSurfaceInfo(physicalDevice, surface)
        }

    /**
     * The selected physical device that best meets the application's requirements.
     * Chosen from available devices based on capabilities and performance characteristics.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val physicalDeviceSurfaceInfo =
        physicalDeviceSurfaceInfos.pickBestDeviceSurfaceInfo().also { deviceSurfaceInfos ->
            logger.info("Selected best physical device: $deviceSurfaceInfos")
        }

    /**
     * The logical device providing the main interface for interacting with the physical device.
     * Created with specific queue families and features enabled for the application's needs.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val logicalDevice =
        LogicalDeviceFactory.create(this, physicalDeviceSurfaceInfo)

    /**
     * The swap chain managing presentation of rendered images to the surface.
     * Created from the logical device and handles image acquisition, rendering synchronization,
     * and presentation to the display using the selected surface format and present mode.
     * The swap chain dimensions and properties are determined based on the physical device's
     * surface capabilities and the window size.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val swapChain: SwapChain =
        logicalDevice.createSwapChain().also { swapChain ->
            logger.info("Created swap chain: $swapChain")
        }

    override fun destroy() {
        swapChain.close()
        logicalDevice.close()
        surface.close()
        physicalDevices.forEach { it.close() }

        if (useValidationLayers) {
            validationLayerLogger.destroy()
        }

        vkDestroyInstance(instance, null)
    }

    override fun toString(): String = "Vulkan"

    companion object {

        @JvmStatic
        private val logger = logger<VulkanRenderingSystem>()
    }
}
