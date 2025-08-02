package dev.cryptospace.anvil.vulkan.graphics

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorPool
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateDescriptorPool
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize

data class DescriptorPool(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorPool,
) : NativeResource() {

    constructor(device: LogicalDevice, maxSets: Int) : this(device, createDescriptorPool(device, maxSets))

    override fun destroy() {
        vkDestroyDescriptorPool(logicalDevice.handle, handle.value, null)
    }

    companion object {

        private fun createDescriptorPool(device: LogicalDevice, maxSets: Int): VkDescriptorPool {
            MemoryStack.stackPush().use { stack ->
                val uniformBufferPoolSize = VkDescriptorPoolSize.calloc(stack).apply {
                    type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    descriptorCount(maxSets)
                }

                val imageSamplerPoolSize = VkDescriptorPoolSize.calloc(stack).apply {
                    type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    descriptorCount(maxSets)
                }

                val poolSizes = VkDescriptorPoolSize.calloc(2, stack)
                    .put(uniformBufferPoolSize)
                    .put(imageSamplerPoolSize)
                    .flip()

                val createInfo = VkDescriptorPoolCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    pPoolSizes(poolSizes)
                    maxSets(maxSets)
                    flags(0)
                }

                val pDescriptorPool = stack.mallocLong(1)
                vkCreateDescriptorPool(device.handle, createInfo, null, pDescriptorPool)
                    .validateVulkanSuccess("Create descriptor pool", "Failed to create descriptor pool")
                return VkDescriptorPool(pDescriptorPool[0])
            }
        }
    }
}
