package dev.cryptospace.anvil.vulkan.device

import dev.cryptospace.anvil.core.native.NativeResource
import dev.cryptospace.anvil.vulkan.Vulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue

data class LogicalDevice(
    val vulkan: Vulkan,
    val handle: VkDevice,
    val physicalDevice: PhysicalDevice
) : NativeResource() {

    val graphicsQueue =
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocPointer(1)
            vkGetDeviceQueue(handle, physicalDevice.graphicsQueueFamilyIndex, 0, buffer)
            VkQueue(buffer[0], handle)
        }

    override fun destroy() {
        check(vulkan.isAlive) { "Vulkan is already destroyed" }
        vkDestroyDevice(handle, null)
    }

}
