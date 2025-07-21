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
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.SyncObjects
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK10.vkQueueSubmit
import org.lwjgl.vulkan.VK10.vkResetCommandBuffer
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSubmitInfo

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

    val syncObjects: SyncObjects = SyncObjects(logicalDevice)

    override fun drawFrame(): Unit = MemoryStack.stackPush().use { stack ->
        syncObjects.waitForInFlightFence()
        val pImageIndex = stack.mallocInt(1)

        vkAcquireNextImageKHR(
            logicalDevice.handle,
            swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphore.value,
            VK_NULL_HANDLE,
            pImageIndex,
        ).validateVulkanSuccess()

        vkResetCommandBuffer(swapChain.commandBuffer.handle, 0)
        swapChain.recordCommands(pImageIndex[0])

        val waitSemaphores = stack.longs(syncObjects.imageAvailableSemaphore.value)
        val waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        val signalSemaphores = stack.longs(syncObjects.renderFinishedSemaphore.value)
        val commandBuffers = stack.pointers(swapChain.commandBuffer.handle)

        val submitInfo = VkSubmitInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            waitSemaphoreCount(1)
            pWaitSemaphores(waitSemaphores)
            pWaitDstStageMask(waitStages)
            pCommandBuffers(commandBuffers)
            pSignalSemaphores(signalSemaphores)
        }

        vkQueueSubmit(logicalDevice.graphicsQueue, submitInfo, syncObjects.inFlightFence.value)
            .validateVulkanSuccess()

        val swapChains = stack.longs(swapChain.handle.value)
        val presentInto = VkPresentInfoKHR.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            pWaitSemaphores(signalSemaphores)
            swapchainCount(1)
            pSwapchains(swapChains)
            pImageIndices(pImageIndex)
            pResults(null)
        }

        vkQueuePresentKHR(logicalDevice.presentQueue, presentInto)
    }

    override fun destroy() {
        syncObjects.close()
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
