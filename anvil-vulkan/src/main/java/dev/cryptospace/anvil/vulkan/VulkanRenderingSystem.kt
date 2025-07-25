package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Vertex2
import dev.cryptospace.anvil.core.math.toByteBuffer
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.context.VulkanContext
import dev.cryptospace.anvil.vulkan.device.DeviceManager
import dev.cryptospace.anvil.vulkan.graphics.Frame
import dev.cryptospace.anvil.vulkan.graphics.SyncObjects
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import java.util.EnumSet

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

    /**
     * Buffer manager for creating and managing Vulkan buffers.
     * Handles vertex buffer creation and memory management.
     */
    private val bufferManager = BufferManager(deviceManager.logicalDevice)

    /**
     * List of frames used for double buffering rendering operations.
     * Contains 2 frames that are used alternatively to allow concurrent CPU and GPU operations,
     * where one frame can be rendered while another is being prepared.
     * Each frame manages its own command buffers and synchronization objects.
     */
    private val frames = List(2) {
        Frame(deviceManager.logicalDevice, bufferManager)
    }

    private var currentFrameIndex = 0
    private var framebufferResized = false

    init {
        glfwSetFramebufferSizeCallback(glfw.window.handle.value) { _, width, height ->
            framebufferResized = true
            logger.info("Framebuffer resized to $width x $height")
        }
    }

    override fun uploadMesh(vertex2: List<Vertex2>, indices: List<Short>): Mesh {
        val verticesBytes = vertex2.toByteBuffer()
        val indicesBytes = indices.toByteBuffer()

        val stagingVertexBufferResource =
            bufferManager.allocateBuffer(
                verticesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_SRC),
                EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
            ).also { allocation ->
                bufferManager.uploadData(allocation, verticesBytes)
            }

        val vertexBufferResource =
            bufferManager.allocateBuffer(
                verticesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX_BUFFER),
                EnumSet.of(BufferProperties.DEVICE_LOCAL),
            ).also { allocation ->
                bufferManager.transferBuffer(stagingVertexBufferResource, allocation)
            }

        val stagingIndexBufferResource =
            bufferManager.allocateBuffer(
                indicesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_SRC),
                EnumSet.of(BufferProperties.HOST_VISIBLE, BufferProperties.HOST_COHERENT),
            ).also { allocation ->
                bufferManager.uploadData(allocation, indicesBytes)
            }

        val indexBufferResource =
            bufferManager.allocateBuffer(
                indicesBytes.remaining().toLong(),
                EnumSet.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX_BUFFER),
                EnumSet.of(BufferProperties.DEVICE_LOCAL),
            ).also { allocation ->
                bufferManager.transferBuffer(stagingIndexBufferResource, allocation)
            }

        return VulkanMesh(vertexBufferResource, indexBufferResource, indices.size)
    }

    override fun drawFrame(callback: (RenderingContext) -> Unit) = MemoryStack.stackPush().use { stack ->
        val frame = frames[currentFrameIndex]
        frame.syncObjects.waitForInFlightFence()
        val imageIndex = acquireSwapChainImage(stack, frame.syncObjects) ?: return
        frame.syncObjects.resetInFlightFence()
        frame.draw(imageIndex, callback)
        currentFrameIndex = (currentFrameIndex + 1).mod(frames.size)
    }

    private fun acquireSwapChainImage(stack: MemoryStack, syncObjects: SyncObjects): Int? {
        val pImageIndex = stack.mallocInt(1)

        val result = vkAcquireNextImageKHR(
            deviceManager.logicalDevice.handle,
            deviceManager.logicalDevice.swapChain.handle.value,
            Long.MAX_VALUE,
            syncObjects.imageAvailableSemaphores.value,
            VK_NULL_HANDLE,
            pImageIndex,
        )

        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || framebufferResized) {
            framebufferResized = false
            deviceManager.logicalDevice.recreateSwapChain()
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

        bufferManager.close()
        deviceManager.close()
        windowSurface.close()
        vulkanContext.close()
    }

    override fun toString(): String = "Vulkan"

    companion object {

        @JvmStatic
        private val logger = logger<VulkanRenderingSystem>()
    }
}
