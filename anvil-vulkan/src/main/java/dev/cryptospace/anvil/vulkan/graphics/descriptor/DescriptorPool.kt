package dev.cryptospace.anvil.vulkan.graphics.descriptor

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.handle.VkDescriptorPool
import dev.cryptospace.anvil.vulkan.utils.toBuffer
import dev.cryptospace.anvil.vulkan.utils.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
import org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCreateDescriptorPool
import org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool
import org.lwjgl.vulkan.VK12
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize

/**
 * Manages a Vulkan descriptor pool for allocating descriptor sets.
 * A descriptor pool maintains a pool of descriptors from which descriptor sets can be allocated.
 * It contains a collection of pools for each descriptor type (uniform buffers and combined image samplers).
 *
 * @property logicalDevice The logical device this descriptor pool belongs to
 * @property handle The native Vulkan handle for this descriptor pool
 */
data class DescriptorPool(
    private val logicalDevice: LogicalDevice,
    val handle: VkDescriptorPool,
) : NativeResource() {

    /**
     * Creates a new descriptor pool with specified capacity.
     *
     * @param logicalDevice The logical device to create the descriptor pool for
     * @param frameInFlights The maximum number of descriptor sets that can be allocated from this pool
     * @param maxTextureCount The maximum number of texture descriptors that can be allocated from this pool
     */
    constructor(logicalDevice: LogicalDevice, frameInFlights: Int, maxTextureCount: Int) : this(
        logicalDevice,
        createDescriptorPool(logicalDevice, frameInFlights, maxTextureCount),
    )

    override fun destroy() {
        vkDestroyDescriptorPool(logicalDevice.handle, handle.value, null)
    }

    companion object {

        private fun createDescriptorPool(
            device: LogicalDevice,
            frameInFlights: Int,
            maxTextureCount: Int,
        ): VkDescriptorPool {
            MemoryStack.stackPush().use { stack ->
                val poolSizes = listOf(
                    VkDescriptorPoolSize.calloc(stack).apply {
                        type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        descriptorCount(frameInFlights)
                    },
                    VkDescriptorPoolSize.calloc(stack).apply {
                        type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        descriptorCount(maxTextureCount)
                    },
                ).toBuffer { size ->
                    VkDescriptorPoolSize.calloc(size, stack)
                }

                val createInfo = VkDescriptorPoolCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    pPoolSizes(poolSizes)
                    maxSets(frameInFlights + 1)
                    flags(VK12.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT)
                }

                val pDescriptorPool = stack.mallocLong(1)
                vkCreateDescriptorPool(device.handle, createInfo, null, pDescriptorPool)
                    .validateVulkanSuccess("Create descriptor pool", "Failed to create descriptor pool")
                return VkDescriptorPool(pDescriptorPool[0])
            }
        }
    }
}
