package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.core.native.Address
import dev.cryptospace.anvil.core.toStringList
import dev.cryptospace.anvil.core.window.Glfw
import dev.cryptospace.anvil.core.window.Window
import dev.cryptospace.anvil.vulkan.surface.Surface
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Struct
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.VK10.VK_ERROR_DEVICE_LOST
import org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_FEATURE_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_FORMAT_NOT_SUPPORTED
import org.lwjgl.vulkan.VK10.VK_ERROR_FRAGMENTED_POOL
import org.lwjgl.vulkan.VK10.VK_ERROR_INCOMPATIBLE_DRIVER
import org.lwjgl.vulkan.VK10.VK_ERROR_INITIALIZATION_FAILED
import org.lwjgl.vulkan.VK10.VK_ERROR_LAYER_NOT_PRESENT
import org.lwjgl.vulkan.VK10.VK_ERROR_MEMORY_MAP_FAILED
import org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY
import org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_HOST_MEMORY
import org.lwjgl.vulkan.VK10.VK_ERROR_TOO_MANY_OBJECTS
import org.lwjgl.vulkan.VK10.VK_ERROR_UNKNOWN
import org.lwjgl.vulkan.VK10.VK_EVENT_RESET
import org.lwjgl.vulkan.VK10.VK_EVENT_SET
import org.lwjgl.vulkan.VK10.VK_INCOMPLETE
import org.lwjgl.vulkan.VK10.VK_NOT_READY
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.VK_TIMEOUT
import java.nio.IntBuffer

val vulkanResultDisplayNameMap = mapOf(
    VK_SUCCESS to "VK_SUCCESS",
    VK_NOT_READY to "VK_NOT_READY",
    VK_TIMEOUT to "VK_TIMEOUT",
    VK_EVENT_SET to "VK_EVENT_SET",
    VK_EVENT_RESET to "VK_EVENT_RESET",
    VK_INCOMPLETE to "VK_INCOMPLETE",
    VK_ERROR_OUT_OF_HOST_MEMORY to "VK_ERROR_OUT_OF_HOST_MEMORY",
    VK_ERROR_OUT_OF_DEVICE_MEMORY to "VK_ERROR_OUT_OF_DEVICE_MEMORY",
    VK_ERROR_INITIALIZATION_FAILED to "VK_ERROR_INITIALIZATION_FAILED",
    VK_ERROR_DEVICE_LOST to "VK_ERROR_DEVICE_LOST",
    VK_ERROR_MEMORY_MAP_FAILED to "VK_ERROR_MEMORY_MAP_FAILED",
    VK_ERROR_LAYER_NOT_PRESENT to "VK_ERROR_LAYER_NOT_PRESENT",
    VK_ERROR_EXTENSION_NOT_PRESENT to "VK_ERROR_EXTENSION_NOT_PRESENT",
    VK_ERROR_FEATURE_NOT_PRESENT to "VK_ERROR_FEATURE_NOT_PRESENT",
    VK_ERROR_INCOMPATIBLE_DRIVER to "VK_ERROR_INCOMPATIBLE_DRIVER",
    VK_ERROR_TOO_MANY_OBJECTS to "VK_ERROR_TOO_MANY_OBJECTS",
    VK_ERROR_FORMAT_NOT_SUPPORTED to "VK_ERROR_FORMAT_NOT_SUPPORTED",
    VK_ERROR_FRAGMENTED_POOL to "VK_ERROR_FRAGMENTED_POOL",
    VK_ERROR_UNKNOWN to "VK_ERROR_UNKNOWN"
)

fun Glfw.getRequiredVulkanExtensions(): List<String> {
    val glfwExtensions = glfwGetRequiredInstanceExtensions()
    checkNotNull(glfwExtensions) { "Failed to find list of required Vulkan extensions" }
    val extensionNames = glfwExtensions.toStringList()
    return extensionNames
}

fun Window.createSurface(vulkan: Vulkan): Surface {
    MemoryStack.stackPush().use { stack ->
        val surfaceBuffer = stack.mallocLong(1)
        val result = glfwCreateWindowSurface(vulkan.instance, this.address.handle, null, surfaceBuffer)
        check(result == 0) { "Failed to create window surface" }
        return Surface(vulkan = vulkan, window = this, address = Address(surfaceBuffer[0]))
    }
}

fun Int.validateVulkanSuccess() {
    check(this == VK_SUCCESS) {
        val resultDisplayName = vulkanResultDisplayNameMap[this] ?: this.toString()
        "Vulkan call was not successful: $resultDisplayName"
    }
}

inline fun getVulkanPointerBuffer(
    bufferQuery: MemoryStack.(IntBuffer, PointerBuffer?) -> Int
): PointerBuffer? {
    return MemoryStack.stackPush().use { stack ->
        val countBuffer = stack.mallocInt(1)
        stack.bufferQuery(countBuffer, null).validateVulkanSuccess()

        if (countBuffer[0] == 0) {
            return null
        }

        val buffer = stack.mallocPointer(countBuffer[0])
        stack.bufferQuery(countBuffer, buffer).validateVulkanSuccess()

        buffer
    }
}

inline fun getVulkanBuffer(
    bufferQuery: MemoryStack.(IntBuffer, IntBuffer?) -> Int
): IntBuffer? {
    return MemoryStack.stackPush().use { stack ->
        val countBuffer = stack.mallocInt(1)
        var result = stack.bufferQuery(countBuffer, null)
        check(result == VK_SUCCESS) { "Vulkan call was not successful" }

        if (countBuffer[0] == 0) {
            return null
        }

        val buffer = stack.ints(countBuffer[0])
        result = stack.bufferQuery(countBuffer, buffer)
        check(result == VK_SUCCESS) { "Vulkan call was not successful" }

        buffer
    }
}

inline fun <T : Struct<T>, B : StructBuffer<T, B>> getVulkanBuffer(
    bufferInitializer: (Int) -> B,
    bufferQuery: MemoryStack.(IntBuffer, B?) -> Int
): B {
    return MemoryStack.stackPush().use { stack ->
        val countBuffer = stack.mallocInt(1)
        var result = stack.bufferQuery(countBuffer, null)
        check(result == VK_SUCCESS) { "Vulkan call was not successful" }

        if (countBuffer[0] == 0) {
            return bufferInitializer(0)
        }

        val buffer = bufferInitializer(countBuffer[0])
        result = stack.bufferQuery(countBuffer, buffer)
        check(result == VK_SUCCESS) { "Vulkan call was not successful" }

        buffer
    }
}
