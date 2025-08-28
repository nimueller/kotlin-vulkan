package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vertex
import dev.cryptospace.anvil.core.rendering.RenderingContext
import dev.cryptospace.anvil.core.scene.Material
import dev.cryptospace.anvil.core.scene.MaterialId
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.scene.TextureId
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.vulkan.buffer.Allocator
import dev.cryptospace.anvil.vulkan.buffer.BufferManager
import dev.cryptospace.anvil.vulkan.device.DeviceManager
import dev.cryptospace.anvil.vulkan.frame.Frame
import dev.cryptospace.anvil.vulkan.frame.FrameDrawResult
import dev.cryptospace.anvil.vulkan.graphics.CommandPool
import dev.cryptospace.anvil.vulkan.graphics.RenderPass
import dev.cryptospace.anvil.vulkan.graphics.SwapChain
import dev.cryptospace.anvil.vulkan.image.Image
import dev.cryptospace.anvil.vulkan.image.TextureManager
import dev.cryptospace.anvil.vulkan.mesh.VulkanDrawLoop
import dev.cryptospace.anvil.vulkan.pipeline.PipelineManager
import dev.cryptospace.anvil.vulkan.pipeline.descriptor.DescriptorSetManager
import dev.cryptospace.anvil.vulkan.pipeline.shader.ShaderManager
import dev.cryptospace.anvil.vulkan.surface.Surface
import dev.cryptospace.anvil.vulkan.utils.getRequiredVulkanExtensions
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT
import org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import java.nio.ByteBuffer
import kotlin.reflect.KClass

const val MAX_TEXTURE_COUNT = 1024

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
    private val context: VulkanContext = VulkanContext(extensions = glfw.getRequiredVulkanExtensions())

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
    private val deviceManager: DeviceManager = DeviceManager(vulkanContext = context, surface = windowSurface)

    /**
     * Memory allocator for Vulkan resources.
     * Handles allocation and management of device memory for buffers, images, and other resources.
     * Provides efficient memory management and tracks allocations to prevent memory leaks.
     */
    private val allocator: Allocator = Allocator(context = context, logicalDevice = deviceManager.logicalDevice)

    /**
     * Command pool for allocating command buffers used to record and submit Vulkan commands.
     * Created from the logical device and manages memory for command buffer allocation and deallocation.
     * Command buffers from this pool are used for recording rendering and compute commands.
     */
    private val commandPool: CommandPool = CommandPool(logicalDevice = deviceManager.logicalDevice)

    /**
     * Buffer manager for creating and managing Vulkan buffers.
     * Handles vertex buffer creation and memory management.
     */
    private val bufferManager: BufferManager =
        BufferManager(
            allocator = allocator,
            logicalDevice = deviceManager.logicalDevice,
            commandPool = commandPool,
        )

    /**
     * Texture manager for creating and managing Vulkan textures and images.
     * Handles texture loading, allocation, and image memory management.
     * Supports uploading texture data from byte buffers and creating image views/samplers.
     * Uses the allocator for device memory allocation and buffer manager for staging operations.
     */
    private val textureManager: TextureManager =
        TextureManager(
            allocator = allocator,
            logicalDevice = deviceManager.logicalDevice,
            bufferManager = bufferManager,
            commandPool = commandPool,
        )

    private val descriptorSetManager: DescriptorSetManager = DescriptorSetManager(deviceManager.logicalDevice)

    private val shaderManager: ShaderManager = ShaderManager(logicalDevice = deviceManager.logicalDevice)

    private val renderPass: RenderPass = RenderPass(deviceManager.logicalDevice)

    private val pipelineManager: PipelineManager =
        PipelineManager(
            logicalDevice = deviceManager.logicalDevice,
            renderPass = renderPass,
            descriptorSetManager = descriptorSetManager,
            shaderManager = shaderManager,
        )

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
        val descriptorSet = descriptorSetManager.frameDescriptorSet.descriptorSet[index]
        Frame(
            deviceManager.logicalDevice,
            bufferManager,
            textureManager,
            descriptorSet,
            descriptorSetManager.textureDescriptorSet.descriptorSet.handles[0],
            commandPool,
            renderPass,
            this,
            pipelineManager.pipelineTextured3D,
        )
    }

    private val drawLoop = VulkanDrawLoop(
        deviceManager.logicalDevice,
        bufferManager,
        textureManager,
        pipelineManager.pipelineTextured3D,
        descriptorSetManager.textureDescriptorSet.descriptorSet.handles[0],
        MAX_TEXTURE_COUNT,
    )

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

    override fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId =
        shaderManager.uploadShader(shaderCode, shaderType)

    override fun uploadImage(imageSize: Int, width: Int, height: Int, imageData: ByteBuffer): TextureId {
        textureManager.allocateImage(
            Image.CreateInfo(
                width = width,
                height = height,
                format = VK10.VK_FORMAT_R8G8B8A8_SRGB,
                usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT,
            ),
        ).also { image ->
            val texture = textureManager.uploadImage(image, imageData)
            return drawLoop.addTexture(texture)
        }
    }

    override fun uploadMaterial(material: Material): MaterialId = drawLoop.addMaterial(material)

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
                drawLoop.drawScene(stack, commandBuffer, engine.scene)
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

        pipelineManager.close()
        shaderManager.close()
        descriptorSetManager.close()
        textureManager.close()
        bufferManager.close()

        renderPass.close()

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
