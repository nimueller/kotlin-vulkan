package dev.cryptospace.anvil.vulkan.pipeline.shader

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import dev.cryptospace.anvil.vulkan.device.LogicalDevice

class ShaderManager(
    private val logicalDevice: LogicalDevice,
) : NativeResource() {

    private val shaders = mutableListOf<RegisteredShader>()

    fun uploadShader(shaderCode: ByteArray, shaderType: ShaderType): ShaderId {
        val shaderModule = ShaderModule(logicalDevice, shaderCode)
        val index = shaders.size
        val registeredShader = RegisteredShader(ShaderId(index.toLong()), shaderModule, shaderType)
        shaders.add(registeredShader)
        return ShaderId(index.toLong())
    }

    fun getRegisteredShader(shaderId: ShaderId): RegisteredShader = shaders[shaderId.value.toInt()]

    override fun destroy() {
        for (shaders in shaders) {
            shaders.shaderModule.close()
        }
    }

    data class RegisteredShader(
        val shaderId: ShaderId,
        val shaderModule: ShaderModule,
        val shaderType: ShaderType,
    )
}
