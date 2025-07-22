package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.AppConfig
import dev.cryptospace.anvil.core.AppConfig.useValidationLayers
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.device.LogicalDeviceFactory
import dev.cryptospace.anvil.vulkan.device.PhysicalDevice
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.device.PhysicalDeviceSurfaceInfo.Companion.pickBestDeviceSurfaceInfo
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.Frame
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.SyncObjects
import dev.cryptospace.anvil.vulkan.handle.VkBuffer
import dev.cryptospace.anvil.vulkan.handle.VkDeviceMemory
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayerLogger
import dev.cryptospace.anvil.vulkan.validation.VulkanValidationLayers
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
import org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO
import org.lwjgl.vulkan.VK10.vkAllocateMemory
import org.lwjgl.vulkan.VK10.vkBindBufferMemory
import org.lwjgl.vulkan.VK10.vkCreateBuffer
import org.lwjgl.vulkan.VK10.vkDestroyBuffer
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import org.lwjgl.vulkan.VK10.vkFreeMemory
import org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VK10.vkMapMemory
import org.lwjgl.vulkan.VK10.vkUnmapMemory
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties

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
    val physicalDevices = PhysicalDevice.listPhysicalDevices(this)

    /**
     * The Vulkan surface used for rendering to the window.
     * Created from the GLFW window and provides the interface between Vulkan and the window system.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val surface = glfw.window.createSurface(this).also { surface ->
        logger.info("Created surface: $surface")
    }

    /**
     * List of surface information details for each available physical device.
     * Contains capabilities, formats, and presentation modes supported by each device
     * when working with the current surface.
     */
    @Suppress("MemberVisibilityCanBePrivate") // may be used in the future
    val physicalDeviceSurfaceInfos = physicalDevices.map { physicalDevice ->
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
    val logicalDevice = LogicalDeviceFactory.create(this, physicalDeviceSurfaceInfo)

    val renderPass: RenderPass = RenderPass.create(logicalDevice)

    val graphicsPipeline: GraphicsPipeline =
        GraphicsPipeline(logicalDevice, renderPass).also { graphicsPipeline ->
            logger.info("Created graphics pipeline: $graphicsPipeline")
        }

    /**
     * The swap chain managing presentation of rendered images to the surface.
     * Created from the logical device and handles image acquisition, rendering synchronization,
     * and presentation to the display using the selected surface format and present mode.
     * The swap chain dimensions and properties are determined based on the physical device's
     * surface capabilities and the window size.
     */
    private var swapChain: SwapChain =
        logicalDevice.createSwapChain(renderPass).also { swapChain ->
            logger.info("Created swap chain: $swapChain")
        }

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    private val commandPool: CommandPool =
        CommandPool.create(logicalDevice).also { commandPool ->
            logger.info("Created command pool: $commandPool")
        }

    private val vertices = listOf(
        Vertex2(Vec2(0.0f, -0.5f), Vec3(1.0f, 0.0f, 0.0f)),
        Vertex2(Vec2(0.5f, 0.5f), Vec3(0.0f, 1.0f, 0.0f)),
        Vertex2(Vec2(-0.5f, 0.5f), Vec3(0.0f, 0.0f, 1.0f)),
    )
    private val verticesBufferSize = vertices.size * Vertex2.SIZE.toLong()
    private val vertexBuffer = createVertexBuffer()

    private val vertexBufferMemory = createVertexBufferMemory()

    /**
     * List of frames used for double buffering rendering operations.
     * Contains 2 frames that are used alternatively to allow concurrent CPU and GPU operations,
     * where one frame can be rendered while another is being prepared.
     * Each frame manages its own command buffers and synchronization objects.
     */
    private val frames = List(2) {
        Frame(logicalDevice, renderPass, graphicsPipeline, swapChain.images.capacity(), commandPool)
    }

    private var currentFrameIndex = 0
    private var framebufferResized = false

    init {
        glfwSetFramebufferSizeCallback(glfw.window.handle.value) { _, width, height ->
            framebufferResized = true
            logger.info("Framebuffer resized to $width x $height")
        }
    }

    private fun createVertexBuffer() = MemoryStack.stackPush().use { stack ->
        val bufferInfo = VkBufferCreateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            size(verticesBufferSize)
            usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
            sharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }

        val pBuffer = stack.mallocLong(1)
        vkCreateBuffer(logicalDevice.handle, bufferInfo, null, pBuffer)
            .validateVulkanSuccess()
        VkBuffer(pBuffer[0])
    }

    private fun createVertexBufferMemory() = MemoryStack.stackPush().use { stack ->
        val bufferMemoryRequirements = VkMemoryRequirements.calloc(stack)
        vkGetBufferMemoryRequirements(logicalDevice.handle, vertexBuffer.value, bufferMemoryRequirements)

        val memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            allocationSize(bufferMemoryRequirements.size())
            memoryTypeIndex(
                findMemoryType(
                    bufferMemoryRequirements.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                ),
            )
        }

        val pDeviceMemory = stack.mallocLong(1)
        vkAllocateMemory(logicalDevice.handle, memoryAllocateInfo, null, pDeviceMemory)
        vkBindBufferMemory(logicalDevice.handle, vertexBuffer.value, pDeviceMemory[0], 0)

        val verticesBuffer = stack.malloc(verticesBufferSize.toInt())
        vertices.forEach { vertex ->
            verticesBuffer
                .putFloat(vertex.position.x)
                .putFloat(vertex.position.y)
                .putFloat(vertex.color.x)
                .putFloat(vertex.color.y)
                .putFloat(vertex.color.z)
        }
        verticesBuffer.flip()
        val verticesBufferAddress = MemoryUtil.memAddress(verticesBuffer)

        val pMemory = stack.mallocPointer(1)
        vkMapMemory(logicalDevice.handle, pDeviceMemory[0], 0, verticesBufferSize, 0, pMemory)
        MemoryUtil.memCopy(verticesBufferAddress, pMemory[0], verticesBufferSize)
        vkUnmapMemory(logicalDevice.handle, pDeviceMemory[0])

        VkDeviceMemory(pDeviceMemory[0])
    }

    private fun findMemoryType(typeFilter: Int, properties: Int): Int = MemoryStack.stackPush().use { stack ->
        val memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack)
        vkGetPhysicalDeviceMemoryProperties(logicalDevice.physicalDevice.handle, memProperties)

        for (i in 0 until memProperties.memoryTypeCount()) {
            if ((typeFilter and (1 shl i)) != 0 &&
                (memProperties.memoryTypes(i).propertyFlags() and properties) == properties
            ) {
                return i
            }
        }

        error("Could not find a suitable memory type")
    }

    override fun drawFrame() = MemoryStack.stackPush().use { stack ->
        val frame = frames[currentFrameIndex]
        frame.syncObjects.waitForInFlightFence()
        val imageIndex = acquireSwapChainImage(stack, frame.syncObjects) ?: return
        frame.syncObjects.resetInFlightFence()
        frame.draw(swapChain, imageIndex, vertexBuffer, vertices)
        currentFrameIndex = (currentFrameIndex + 1).mod(frames.size)
    }

    private fun acquireSwapChainImage(stack: MemoryStack, syncObjects: SyncObjects): Int? {
        val pImageIndex = stack.mallocInt(1)

        val result = vkAcquireNextImageKHR(
            logicalDevice.handle,
            swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphores.value,
            VK_NULL_HANDLE,
            pImageIndex,
        )

        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || framebufferResized) {
            framebufferResized = false
            swapChain = swapChain.recreate(renderPass)
            return null
        } else {
            result.validateVulkanSuccess()
        }

        return pImageIndex[0]
    }

    override fun destroy() {
        vkDeviceWaitIdle(logicalDevice.handle)
            .validateVulkanSuccess()

        frames.forEach { frame ->
            frame.close()
        }

        commandPool.close()
        swapChain.close()

        vkDestroyBuffer(logicalDevice.handle, vertexBuffer.value, null)
        vkFreeMemory(logicalDevice.handle, vertexBufferMemory.value, null)

        graphicsPipeline.close()
        renderPass.close()
        logicalDevice.close()
        surface.close()
        physicalDeviceSurfaceInfos.forEach { it.close() }
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
