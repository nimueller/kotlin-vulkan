package dev.cryptospace.anvil.vulkan.utils

import dev.cryptospace.anvil.core.math.AttributeFormat
import dev.cryptospace.anvil.core.shader.ShaderType
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import kotlin.test.Test
import kotlin.test.assertEquals

class EngineExtensionsTest {

    @Test
    fun `test AttributeFormat to Vulkan format`() {
        assertEquals(VK_FORMAT_R32G32_SFLOAT, AttributeFormat.SIGNED_FLOAT_2D.toVulkanFormat())
        assertEquals(VK_FORMAT_R32G32B32_SFLOAT, AttributeFormat.SIGNED_FLOAT_3D.toVulkanFormat())
    }

    @Test
    fun `test ShaderType to Vulkan bitmask`() {
        assertEquals(VK_SHADER_STAGE_VERTEX_BIT, ShaderType.VERTEX.vkValue)
        assertEquals(VK_SHADER_STAGE_FRAGMENT_BIT, ShaderType.FRAGMENT.vkValue)
    }

    @Test
    fun `test collection of ShaderType to bitmask`() {
        val stages = listOf(ShaderType.VERTEX, ShaderType.FRAGMENT)
        val expected = VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT
        assertEquals(expected, stages.toBitmask())
    }

    @Test
    fun `test single ShaderType to bitmask`() {
        val stages = listOf(ShaderType.VERTEX)
        assertEquals(VK_SHADER_STAGE_VERTEX_BIT, stages.toBitmask())
    }
}
