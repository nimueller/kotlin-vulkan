package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.buffer.Allocator
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.context.VulkanContext
import dev.cryptospace.anvil.vulkan.device.DeviceManager
import dev.cryptospace.anvil.vulkan.frame.Frame
import dev.cryptospace.anvil.vulkan.frame.FrameDrawResult
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.GraphicsPipeline
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.graphics.descriptor.DescriptorPool
import dev.cryptospace.anvil.vulkan.graphics.descriptor.DescriptorSet
import dev.cryptospace.anvil.vulkan.graphics.descriptor.DescriptorSetLayout
import dev.cryptospace.anvil.vulkan.graphics.descriptor.FrameDescriptorSetLayout
import dev.cryptospace.anvil.vulkan.graphics.descriptor.MaterialDescriptorSetLayout
import dev.cryptospace.anvil.vulkan.image.Image
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.mesh.VulkanDrawLoop
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import java.nio.ByteBuffer
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

    /**
     * The Vulkan context managing the Vulkan instance and extensions.
     * Handles initialization of the Vulkan API and maintains required instance-level functionality.
     * Created with GLFW-required Vulkan extensions to enable window system integration.
     */
    private val context: VulkanContext = VulkanContext(glfw.getRequiredVulkanExtensions())

    /**
     * Surface for presenting rendered images to the window.
     * Creates and manages the Vulkan surface connected to the GLFW window,
     * which is used as the target for rendering operations.
     */
    private val windowSurface: Surface = Surface(vulkanInstance = context.handle, window = glfw.window)

    /**
     * Manager for Vulkan devices and queues.
     * Handles selection of physical device (GPU) and creation of logical device.
     * Manages queue families and provides access to graphics and presentation queues.
     */
    private val deviceManager: DeviceManager = DeviceManager(context, windowSurface)

    /**
     * Memory allocator for Vulkan resources.
     * Handles allocation and management of device memory for buffers, images, and other resources.
     * Provides efficient memory management and tracks allocations to prevent memory leaks.
     */
    private val allocator: Allocator = Allocator(context, deviceManager.logicalDevice)

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    private val commandPool: CommandPool = CommandPool(deviceManager.logicalDevice)

    /**
     * Buffer manager for creating and managing Vulkan buffers.
     * Handles vertex buffer creation and memory management.
     */
    private val bufferManager: BufferManager = BufferManager(allocator, deviceManager.logicalDevice, commandPool)

    /**
     * Texture manager for creating and managing Vulkan textures and images.
     * Handles texture loading, allocation, and image memory management.
     * Supports uploading texture data from byte buffers and creating image views/samplers.
     * Uses the allocator for device memory allocation and buffer manager for staging operations.
     */
    private val textureManager: TextureManager =
        TextureManager(allocator, deviceManager.logicalDevice, bufferManager, commandPool)

    /**
     * Descriptor pool for allocating descriptor sets used in the rendering pipeline.
     * This pool manages the allocation of descriptor sets for both per-frame uniforms and material textures.
     * Configured with capacity for frame-in-flight buffers plus maximum allowed texture descriptors.
     * Supports dynamic updating of descriptors after binding for texture updates during runtime.
     */
    val descriptorPool: DescriptorPool =
        DescriptorPool(
            logicalDevice = deviceManager.logicalDevice,
            frameInFlights = FRAMES_IN_FLIGHT,
            maxTextureCount = MaterialDescriptorSetLayout.MAX_TEXTURE_COUNT,
        )

    val frameDescriptorSetLayout: DescriptorSetLayout = FrameDescriptorSetLayout(deviceManager.logicalDevice)

    val materialDescriptorSetLayout: DescriptorSetLayout = MaterialDescriptorSetLayout(deviceManager.logicalDevice)

    val renderPass: RenderPass = RenderPass(deviceManager.logicalDevice)

    val graphicsPipelineTextured3D: GraphicsPipeline =
        GraphicsPipeline(
            deviceManager.logicalDevice,
            renderPass,
            listOf(frameDescriptorSetLayout, materialDescriptorSetLayout),
            TexturedVertex3,
        )

    /**
     * Descriptor set for frame-specific uniform buffers.
     * Contains descriptor sets allocated for each frame in flight,
     * managing per-frame uniform buffer bindings used for view/projection matrices
     * and other frame-specific data.
     */
    private val frameDescriptorSet: DescriptorSet =
        frameDescriptorSetLayout.createDescriptorSet(descriptorPool, FRAMES_IN_FLIGHT)

    /**
     * Descriptor set for material textures and samplers.
     * Contains a single descriptor set that manages texture bindings
     * used across all materials in the rendering pipeline.
     * Used to bind texture samplers to the fragment shader.
     */
    private val materialDescriptorSet: DescriptorSet =
        materialDescriptorSetLayout.createDescriptorSet(descriptorPool, 1)

    /**
     * The swap chain managing presentation of rendered images to the surface.
     * Created from the logical device and handles image acquisition, rendering synchronization,
     * and presentation to the display using the selected surface format and present mode.
     * The swap chain dimensions and properties are determined based on the physical device's
     * surface capabilities and the window size.
     */
    var swapChain: SwapChain = SwapChain(deviceManager.logicalDevice, textureManager, renderPass)

    /**
     * List of frames used for double buffering rendering operations.
     * Contains 2 frames that are used alternatively to allow concurrent CPU and GPU operations,
     * where one frame can be rendered while another is being prepared.
     * Each frame manages its own command buffers and synchronization objects.
     */
    private val frames: List<Frame> = List(FRAMES_IN_FLIGHT) { index ->
        val descriptorSet = frameDescriptorSet[index]
        Frame(
            deviceManager.logicalDevice,
            bufferManager,
            textureManager,
            descriptorSet,
            materialDescriptorSet[0],
            commandPool,
            renderPass,
            this,
            graphicsPipelineTextured3D,
        )
    }

    private val drawLoop = VulkanDrawLoop(bufferManager, graphicsPipelineTextured3D)

    fun recreateSwapChain() {
        swapChain = swapChain.recreate(renderPass)
    }

    private var currentFrameIndex = 0
    private var framebufferResized = false

    init {
        glfwSetFramebufferSizeCallback(glfw.window.handle.value) { _, width, height ->
            framebufferResized = true
            logger.info("Framebuffer resized to $width x $height")
        }
    }

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer) {
        textureManager.allocateImage(
            Image.CreateInfo(
                width = width,
                height = height,
                format = VK10.VK_FORMAT_R8G8B8A8_SRGB,
                usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            ),
        ).also { image ->
            textureManager.uploadImage(image, imageData)
        }.also {
            frames.forEach { frame ->
                frame.updateDescriptorSets()
            }
        }
    }

    override fun <V : Vertex> uploadMesh(vertexType: KClass<V>, vertices: Array<V>, indices: Array<UInt>): MeshId =
        drawLoop.addMesh(vertexType, vertices, indices)

    override fun drawFrame(engine: Engine, callback: (RenderingContext) -> Unit) =
        MemoryStack.stackPush().use { stack ->
            if (framebufferResized) {
                framebufferResized = false
                recreateSwapChain()
            }

            val frame = frames[currentFrameIndex]
            val result = frame.draw(engine) { commandBuffer, renderingContext ->
                callback(renderingContext)

                for (gameObject in engine.scene.gameObjects) {
                    drawLoop.draw(stack, commandBuffer, gameObject)
                }
            }

            when (result) {
                FrameDrawResult.FRAMEBUFFER_RESIZED -> {
                    framebufferResized = false
                    recreateSwapChain()
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

        swapChain.close()
        graphicsPipelineTextured3D.close()
        renderPass.close()
        frameDescriptorSetLayout.close()
        materialDescriptorSetLayout.close()
        descriptorPool.close()

        textureManager.close()
        bufferManager.close()
        commandPool.close()
        allocator.close()
        deviceManager.close()
        windowSurface.close()
        context.close()
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
