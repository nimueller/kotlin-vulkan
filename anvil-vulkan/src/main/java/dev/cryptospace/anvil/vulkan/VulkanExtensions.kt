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

/**
 * Maps Vulkan result codes to their string representations.
 * This map contains common Vulkan result codes and their corresponding display names,
 * which can be used for error reporting and debugging purposes.
 */
val vulkanResultDisplayNameMap =
    mapOf(
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
        VK_ERROR_UNKNOWN to "VK_ERROR_UNKNOWN",
    )

/**
 * Gets the list of Vulkan extensions required by GLFW.
 * These extensions are necessary for Vulkan to work with GLFW windows.
 *
 * @return List of required Vulkan extension names as strings
 * @throws IllegalStateException if the required extensions cannot be retrieved
 */
fun Glfw.getRequiredVulkanExtensions(): List<String> {
    val glfwExtensions = glfwGetRequiredInstanceExtensions()
    checkNotNull(glfwExtensions) { "Failed to find list of required Vulkan extensions" }
    val extensionNames = glfwExtensions.toStringList()
    return extensionNames
}

/**
 * Creates a Vulkan surface for this window.
 * The surface is used for rendering Vulkan graphics to the window.
 *
 * @param vulkan The Vulkan instance to create the surface for
 * @return A new Surface instance
 * @throws IllegalStateException if the surface creation fails
 */
fun Window.createSurface(vulkan: Vulkan): Surface {
    MemoryStack.stackPush().use { stack ->
        val surfaceBuffer = stack.mallocLong(1)
        val result = glfwCreateWindowSurface(vulkan.instance, this.address.handle, null, surfaceBuffer)
        check(result == 0) { "Failed to create window surface" }
        return Surface(vulkan = vulkan, window = this, address = Address(surfaceBuffer[0]))
    }
}

/**
 * Validates that a Vulkan operation was successful.
 * Throws an exception if the result code is not [VK_SUCCESS].
 *
 * @throws IllegalStateException if the result code is not VK_SUCCESS, with a message containing
 * the string representation of the error code
 */
fun Int.validateVulkanSuccess() {
    check(this == VK_SUCCESS) {
        val resultDisplayName = vulkanResultDisplayNameMap[this] ?: this.toString()
        "Vulkan call was not successful: $resultDisplayName"
    }
}

/**
 * Fetches a Vulkan pointer buffer using the provided query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the result.
 *
 * Example:
 * ```kotlin
 * val extensions = getVulkanPointerBuffer { countBuffer, resultBuffer ->
 *     vkEnumerateInstanceExtensionProperties(
 *         null as String?,
 *         countBuffer,
 *         resultBuffer
 *     )
 * }
 * ```
 *
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to `countBuffer`).
 *   2. Second, with an appropriately sized `resultBuffer` to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional `PointerBuffer` to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length arrays.
 *
 * @return A `PointerBuffer` containing the requested data if successful, or `null` if no data was available.
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun getVulkanPointerBuffer(bufferQuery: MemoryStack.(IntBuffer, PointerBuffer?) -> Int): PointerBuffer? {
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

/**
 * Fetches a Vulkan integer buffer using the provided query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the result.
 *
 * Example:
 * ```kotlin
 * val presentModes = getVulkanBuffer { countBuffer, resultBuffer ->
 *     vkGetPhysicalDeviceSurfacePresentModesKHR(
 *         physicalDevice.handle,
 *         surface.address.handle,
 *         countBuffer,
 *         resultBuffer
 *     )
 * }
 * ```
 *
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to `countBuffer`).
 *   2. Second, with an appropriately sized `resultBuffer` to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional `IntBuffer` to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length arrays.
 *
 * @return An `IntBuffer` containing the requested data if successful, or `null` if no data was available.
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun getVulkanBuffer(bufferQuery: MemoryStack.(IntBuffer, IntBuffer?) -> Int): IntBuffer? {
    return MemoryStack.stackPush().use { stack ->
        val countBuffer = stack.mallocInt(1)
        stack.bufferQuery(countBuffer, null).validateVulkanSuccess()

        if (countBuffer[0] == 0) {
            return null
        }

        val buffer = stack.mallocInt(countBuffer[0])
        stack.bufferQuery(countBuffer, buffer).validateVulkanSuccess()

        buffer
    }
}

/**
 * Fetches a Vulkan struct buffer using the provided buffer initializer and query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the result.
 *
 * Example:
 * ```kotlin
 * val formats = getVulkanBuffer(
 *     bufferInitializer = { VkSurfaceFormatKHR.malloc(it) },
 *     bufferQuery = { countBuffer, resultBuffer ->
 *         vkGetPhysicalDeviceSurfaceFormatsKHR(
 *             physicalDevice.handle,
 *             surface.address.handle,
 *             countBuffer,
 *             resultBuffer
 *         )
 *     }
 * )
 * ```
 *
 * @param T The type of Vulkan struct being allocated.
 * @param B The type of struct buffer being created.
 * @param bufferInitializer A function that creates a new struct buffer of the specified size.
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to `countBuffer`).
 *   2. Second, with a newly allocated result buffer to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional struct buffer to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length arrays.
 *
 * @return A struct buffer containing the requested data if successful, or an empty buffer if no data was available.
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun <T : Struct<T>, B : StructBuffer<T, B>> getVulkanBuffer(
    bufferInitializer: (Int) -> B,
    bufferQuery: MemoryStack.(IntBuffer, B?) -> Int,
): B {
    return MemoryStack.stackPush().use { stack ->
        val countBuffer = stack.mallocInt(1)
        stack.bufferQuery(countBuffer, null).validateVulkanSuccess()

        val count = countBuffer[0]
        if (count == 0) {
            return bufferInitializer(0)
        }

        val buffer = bufferInitializer(count)
        stack.bufferQuery(countBuffer, buffer).validateVulkanSuccess()
        buffer
    }
}
