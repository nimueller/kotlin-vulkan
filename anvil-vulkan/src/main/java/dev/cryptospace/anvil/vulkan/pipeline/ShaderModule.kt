package dev.cryptospace.anvil.vulkan.pipeline

import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkShaderModuleCreateInfo

/**
 * Represents a Vulkan shader module that encapsulates compiled shader code.
 * This class manages the lifecycle of a shader module, including creation from shader bytecode
 * and cleanup of resources when no longer needed.
 *
 * @property logicalDevice The logical device that owns this shader module
 * @property handle The native Vulkan shader module handle
 */
class ShaderModule(
    private val logicalDevice: LogicalDevice,
    val handle: VkShaderModule,
) : NativeResource() {

    /**
     * Creates a shader module from shader bytecode loaded from the specified resource path.
     *
     * @param logicalDevice The logical device that will own this shader module
     * @param path The resource path to the shader bytecode file
     * @throws IllegalStateException if the shader file cannot be found at the specified path
     */
    constructor(logicalDevice: LogicalDevice, path: String) : this(logicalDevice, createShader(logicalDevice, path))

    override fun destroy() {
        VK10.vkDestroyShaderModule(logicalDevice.handle, handle.value, null)
    }

    companion object {

        @JvmStatic
        private val log = logger<ShaderModule>()

        private fun createShader(logicalDevice: LogicalDevice, path: String): VkShaderModule =
            MemoryStack.stackPush().use { stack ->
                val shaderCode = ShaderModule::class.java.getResourceAsStream(path)?.readAllBytes()
                    ?: error("Shader not found at classpath: $path")
                val shaderCodeBuffer = stack.malloc(shaderCode.size)
                shaderCodeBuffer.put(shaderCode)
                shaderCodeBuffer.flip()

                val createInfo = VkShaderModuleCreateInfo.calloc(stack).apply {
                    sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    pCode(shaderCodeBuffer)
                }

                val shaderModule = stack.mallocLong(1)
                VK10.vkCreateShaderModule(logicalDevice.handle, createInfo, null, shaderModule)
                    .validateVulkanSuccess("Create shader module", "Failed to create shader module for $path")

                VkShaderModule(shaderModule[0]).also { handle ->
                    log.debug { "Created shader module: $handle" }
                }
            }
    }
}
