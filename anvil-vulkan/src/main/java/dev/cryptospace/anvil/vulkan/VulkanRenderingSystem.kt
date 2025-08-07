package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.image.Texture
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.*
import dev.cryptospace.anvil.core.rendering.Mesh
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.buffer.BufferProperties
import dev.cryptospace.anvil.vulkan.buffer.BufferUsage
import dev.cryptospace.anvil.vulkan.context.VulkanContext
import dev.cryptospace.anvil.vulkan.device.DeviceManager
import dev.cryptospace.anvil.vulkan.frame.Frame
import dev.cryptospace.anvil.vulkan.frame.FrameDrawResult
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSet
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KClass

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

    private val textureManager = TextureManager(deviceManager.logicalDevice, bufferManager)

    private val descriptorSets: List<VkDescriptorSet> = MemoryStack.stackPush().use { stack ->
        val logicalDevice = deviceManager.logicalDevice

        val pDescriptorPools = stack.mallocLong(1)
        pDescriptorPools.put(0, logicalDevice.descriptorPool.handle.value)

        val setLayouts = stack.mallocLong(FRAMES_IN_FLIGHT)

        for (i in 0 until FRAMES_IN_FLIGHT) {
            setLayouts.put(logicalDevice.descriptorSetLayout.handle.value)
        }

        setLayouts.flip()

        val setAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).apply {
            sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            descriptorPool(pDescriptorPools[0])
            pSetLayouts(setLayouts)
        }

        val pDescriptorSets = stack.mallocLong(FRAMES_IN_FLIGHT)
        vkAllocateDescriptorSets(logicalDevice.handle, setAllocateInfo, pDescriptorSets)
            .validateVulkanSuccess("Allocate descriptor sets", "Failed to allocate descriptor sets")
        List(FRAMES_IN_FLIGHT) {
            VkDescriptorSet(pDescriptorSets[it])
        }
    }

    /**
     * List of frames used for double buffering rendering operations.
     * Contains 2 frames that are used alternatively to allow concurrent CPU and GPU operations,
     * where one frame can be rendered while another is being prepared.
     * Each frame manages its own command buffers and synchronization objects.
     */
    private val frames: List<Frame> = List(FRAMES_IN_FLIGHT) { index ->
        val descriptorSet = descriptorSets[index]
        Frame(deviceManager.logicalDevice, bufferManager, textureManager, descriptorSet)
    }

    private var currentFrameIndex = 0
    private var framebufferResized = false

    init {
        glfwSetFramebufferSizeCallback(glfw.window.handle.value) { _, width, height ->
            framebufferResized = true
            logger.info("Framebuffer resized to $width x $height")
        }
    }

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): Texture =
        textureManager.uploadImage(imageSize, width, height, imageData).also {
            frames.forEach { frame ->
                frame.updateDescriptorSets()
            }
        }

    override fun <V : Vertex> uploadMesh(vertexType: KClass<V>, vertices: List<V>, indices: List<Short>): Mesh {
        val verticesBytes = vertices.toByteBuffer()
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

        val graphicsPipeline = when (vertexType) {
            TexturedVertex2::class -> deviceManager.logicalDevice.graphicsPipelineTextured2D
            TexturedVertex3::class -> deviceManager.logicalDevice.graphicsPipelineTextured3D
            else -> error("Unsupported vertex type: $vertexType")
        }

        return VulkanMesh(
            modelMatrix = Mat4.identity,
            vertexBufferResource,
            indexBufferResource,
            indices.size,
            graphicsPipeline,
        )
    }

    override fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit) {
        if (framebufferResized) {
            framebufferResized = false
            deviceManager.logicalDevice.recreateSwapChain()
        }

        val frame = frames[currentFrameIndex]
        val result = frame.draw(engine, callback)

        when (result) {
            FrameDrawResult.FRAMEBUFFER_RESIZED -> {
                framebufferResized = false
                deviceManager.logicalDevice.recreateSwapChain()
            }

            FrameDrawResult.SUCCESS -> {
                currentFrameIndex = (currentFrameIndex + 1).mod(frames.size)
            }
        }
    }

    override fun destroy() {
        vkDeviceWaitIdle(deviceManager.logicalDevice.handle)
            .validateVulkanSuccess()

        frames.forEach { frame ->
            frame.close()
        }

        textureManager.close()
        bufferManager.close()
        deviceManager.close()
        windowSurface.close()
        vulkanContext.close()
    }

    override fun perspective(fov: Float, aspect: Float, near: Float, far: Float): Mat4 =
        Mat4.perspectiveVulkan(fov, aspect, near, far)

    override fun toString(): String = "Vulkan"

    companion object {

        @JvmStatic
        private val logger = logger<VulkanRenderingSystem>()

        const val FRAMES_IN_FLIGHT = 2
    }
}
