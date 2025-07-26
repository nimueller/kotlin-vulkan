package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorSetLayout
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo

data class DescriptorSetLayout(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorSetLayout,
) : NativeResource() {

    constructor(logicalDevice: LogicalDevice) : this(
        logicalDevice,
        createShaderDescriptor(logicalDevice),
    )

    override fun destroy() {
        vkDestroyDescriptorSetLayout(logicalDevice.handle, handle.value, null)
    }

    companion object {

        private fun createShaderDescriptor(logicalDevice: LogicalDevice): VkDescriptorSetLayout =
            MemoryStack.stackPush().use { stack ->
                val layoutBinding = VkDescriptorSetLayoutBinding.calloc(stack).apply {
                    binding(0)
                    descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    descriptorCount(1)
                    stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    pImmutableSamplers(null)
                }

                val layoutBindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
                    .put(layoutBinding)
                    .flip()

                val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).apply {
                    sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    pBindings(layoutBindings)
                }

                val pSetLayout = stack.mallocLong(1)
                vkCreateDescriptorSetLayout(logicalDevice.handle, createInfo, null, pSetLayout)
                    .validateVulkanSuccess("Create descriptor set layout", "Failed to create descriptor set layout")
                VkDescriptorSetLayout(pSetLayout[0])
            }
    }
}
