package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.context.VulkanContext
import dev.cryptospace.anvil.vulkan.device.DeviceManager
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.Frame
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.SyncObjects
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle

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

    private val vulkanContext: VulkanContext = VulkanContext(glfw.getRequiredVulkanExtensions())

    private val windowSurface: Surface = Surface(vulkanInstance = vulkanContext.handle, window = glfw.window)

    private val deviceManager: DeviceManager = DeviceManager(vulkanContext, windowSurface)

    private val renderPass: RenderPass = RenderPass(deviceManager.logicalDevice)

    private val graphicsPipeline: GraphicsPipeline = GraphicsPipeline(deviceManager.logicalDevice, renderPass)

    /**
     * The swap chain managing presentation of rendered images to the surface.
     * Created from the logical device and handles image acquisition, rendering synchronization,
     * and presentation to the display using the selected surface format and present mode.
     * The swap chain dimensions and properties are determined based on the physical device's
     * surface capabilities and the window size.
     */
    private var swapChain: SwapChain = SwapChain(deviceManager.logicalDevice, renderPass)

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    private val commandPool: CommandPool = CommandPool(deviceManager.logicalDevice)

    private val vertices = listOf(
        Vertex2(Vec2(0.0f, -0.5f), Vec3(1.0f, 0.0f, 0.0f)),
        Vertex2(Vec2(0.5f, 0.5f), Vec3(0.0f, 1.0f, 0.0f)),
        Vertex2(Vec2(-0.5f, 0.5f), Vec3(0.0f, 0.0f, 1.0f)),
    )

    /**
     * Buffer manager for creating and managing Vulkan buffers.
     * Handles vertex buffer creation and memory management.
     */
    private val bufferManager = BufferManager(deviceManager.logicalDevice)

    /**
     * Vertex buffer resource containing the buffer and its associated memory.
     * Created by the buffer manager and used for rendering.
     */
    private val vertexBufferResource = bufferManager.createVertexBuffer(vertices)

    /**
     * List of frames used for double buffering rendering operations.
     * Contains 2 frames that are used alternatively to allow concurrent CPU and GPU operations,
     * where one frame can be rendered while another is being prepared.
     * Each frame manages its own command buffers and synchronization objects.
     */
    private val frames = List(2) {
        Frame(deviceManager.logicalDevice, renderPass, graphicsPipeline, swapChain.images.capacity(), commandPool)
    }

    private var currentFrameIndex = 0
    private var framebufferResized = false

    init {
        glfwSetFramebufferSizeCallback(glfw.window.handle.value) { _, width, height ->
            framebufferResized = true
            logger.info("Framebuffer resized to $width x $height")
        }
    }

    override fun drawFrame() = MemoryStack.stackPush().use { stack ->
        val frame = frames[currentFrameIndex]
        frame.syncObjects.waitForInFlightFence()
        val imageIndex = acquireSwapChainImage(stack, frame.syncObjects) ?: return
        frame.syncObjects.resetInFlightFence()
        frame.draw(swapChain, imageIndex, vertexBufferResource.buffer, vertices)
        currentFrameIndex = (currentFrameIndex + 1).mod(frames.size)
    }

    private fun acquireSwapChainImage(stack: MemoryStack, syncObjects: SyncObjects): Int? {
        val pImageIndex = stack.mallocInt(1)

        val result = vkAcquireNextImageKHR(
            deviceManager.logicalDevice.handle,
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
        vkDeviceWaitIdle(deviceManager.logicalDevice.handle)
            .validateVulkanSuccess()

        frames.forEach { frame ->
            frame.close()
        }

        commandPool.close()
        swapChain.close()
        bufferManager.close()
        graphicsPipeline.close()
        renderPass.close()
        windowSurface.close()
        deviceManager.close()
        vulkanContext.close()
    }

    override fun toString(): String = "Vulkan"

    companion object {

        @JvmStatic
        private val logger = logger<VulkanRenderingSystem>()
    }
}
