package dev.cryptospace.anvil.vulkan.buffer

import dev.cryptospace.anvil.core.debug
import dev.cryptospace.anvil.core.logger
import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.VulkanContext
import dev.cryptospace.anvil.vulkan.device.LogicalDevice
import dev.cryptospace.anvil.vulkan.validateVulkanSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions

/**
 * Wrapper for Vulkan Memory Allocator (VMA) allocator handle.
 * Manages memory allocation for Vulkan buffers and images using the VMA library.
 */
class Allocator(
    val handle: VmaAllocator,
) : NativeResource() {

    /**
     * Creates a new VMA allocator for the specified Vulkan context and logical device.
     *
     * @param context The Vulkan context.
     * @param logicalDevice The logical device to allocate memory from.
     */
    constructor(context: VulkanContext, logicalDevice: LogicalDevice) : this(
        create(context, logicalDevice),
    )

    override fun destroy() {
        Vma.vmaDestroyAllocator(handle.value)
    }

    companion object {

        private val logger = logger<Allocator>()

        private fun create(context: VulkanContext, logicalDevice: LogicalDevice) =
            MemoryStack.stackPush().use { stack ->
                val functions = VmaVulkanFunctions.calloc(stack).apply {
                    set(context.handle, logicalDevice.handle)
                }

                val createInfo = VmaAllocatorCreateInfo.calloc(stack).apply {
                    flags(0)
                    physicalDevice(logicalDevice.physicalDevice.handle)
                    device(logicalDevice.handle)
                    instance(context.handle)
                    vulkanApiVersion(VulkanContext.API_VERSION)
                    pVulkanFunctions(functions)
                    pAllocationCallbacks(null)
                }

                val pAllocator = stack.mallocPointer(1)
                Vma.vmaCreateAllocator(createInfo, pAllocator)
                    .validateVulkanSuccess("Create allocator", "Failed to create allocator")
                VmaAllocator(pAllocator[0]).also { allocator ->
                    logger.debug { "Created allocator: $allocator" }
                }
            }
    }
}
