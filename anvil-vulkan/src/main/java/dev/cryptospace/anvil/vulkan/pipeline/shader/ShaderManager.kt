package dev.cryptospace.anvil.vulkan.pipeline.shader

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice

/**
 * Manages shader resources in the Vulkan context, handling shader module creation,
 * registration, and lifecycle management.
 *
 * @property logicalDevice The logical device used for shader operations
 */
class ShaderManager(
    private val logicalDevice: LogicalDevice,
) : NativeResource() {

    private val shaders = mutableListOf<RegisteredShader>()

    /**
     * Uploads and registers a new shader module.
     *
     * @param shaderCode The binary SPIR-V shader code
     * @param shaderType The type of shader (vertex, fragment, etc.)
     * @return The unique identifier for the registered shader
     */
    fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId {
        val shaderModule = ShaderModule(logicalDevice, shaderCode)
        val index = shaders.size
        val registeredShader = RegisteredShader(ShaderId(index.toLong()), shaderModule, shaderType)
        shaders.add(registeredShader)
        return ShaderId(index.toLong())
    }

    /**
     * Retrieves a registered shader by its identifier.
     *
     * @param shaderId The unique identifier of the shader
     * @return The registered shader information
     */
    fun getRegisteredShader(shaderId: ShaderId): RegisteredShader = shaders[shaderId.value.toInt()]

    override fun destroy() {
        for (shaders in shaders) {
            shaders.shaderModule.close()
        }
    }

    /**
     * Data class representing a registered shader in the manager.
     *
     * @property shaderId The unique identifier of the shader
     * @property shaderModule The Vulkan shader module
     * @property shaderType The type of shader
     */
    data class RegisteredShader(
        val shaderId: ShaderId,
        val shaderModule: ShaderModule,
        val shaderType: ShaderType,
    )
}
