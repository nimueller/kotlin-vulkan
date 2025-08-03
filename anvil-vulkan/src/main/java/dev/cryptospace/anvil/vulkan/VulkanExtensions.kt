package dev.cryptospace.anvil.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Struct
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.EXTBufferDeviceAddress.VK_ERROR_INVALID_DEVICE_ADDRESS_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT
import org.lwjgl.vulkan.EXTFullScreenExclusive.VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT
import org.lwjgl.vulkan.EXTGlobalPriority.VK_ERROR_NOT_PERMITTED_EXT
import org.lwjgl.vulkan.EXTImageDrmFormatModifier.VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT
import org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV
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
        VK_SUBOPTIMAL_KHR to "VK_SUBOPTIMAL_KHR",
        VK_ERROR_OUT_OF_DATE_KHR to "VK_ERROR_OUT_OF_DATE_KHR",
        VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT to "VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT",
        VK_ERROR_VALIDATION_FAILED_EXT to "VK_ERROR_VALIDATION_FAILED_EXT",
        VK_ERROR_INVALID_SHADER_NV to "VK_ERROR_INVALID_SHADER_NV",
        VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT to
            "VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT",
        VK_ERROR_NOT_PERMITTED_EXT to "VK_ERROR_NOT_PERMITTED_EXT",
        VK_ERROR_INVALID_DEVICE_ADDRESS_EXT to "VK_ERROR_INVALID_DEVICE_ADDRESS_EXT",
        VK_ERROR_INCOMPATIBLE_DISPLAY_KHR to "VK_ERROR_INCOMPATIBLE_DISPLAY_KHR",
        VK_ERROR_VALIDATION_FAILED_EXT to "VK_ERROR_VALIDATION_FAILED_EXT",
    )

/**
 * Validates that a Vulkan operation was successful.
 * Throws an appropriate VulkanException if the result code is not [VK_SUCCESS].
 *
 * @param operation Optional description of the operation being performed
 * @param message Optional additional error message
 * @throws VulkanException if the result code is not VK_SUCCESS
 */
fun Int.validateVulkanSuccess(operation: String? = null, message: String? = null) {
    if (this != VK_SUCCESS) {
        throw dev.cryptospace.anvil.vulkan.exception.createVulkanException(this, operation, message)
    }
}

/**
 * Converts a list of Vulkan structs into a struct buffer.
 * This function takes a list of Vulkan struct objects and creates a buffer containing those structs
 * using the provided buffer creation callback.
 *
 * Example:
 * ```kotlin
 * val structs = listOf(struct1, struct2, struct3)
 * val buffer = structs.toBuffer { size -> VkSomeStruct.malloc(size, stack) }
 * ```
 *
 * @param S The type of Vulkan struct being stored
 * @param B The type of struct buffer being created
 * @param createCallback A function that creates a new struct buffer of the specified size
 * @return A struct buffer containing all the structs from the list
 */
inline fun <S : Struct<S>, B : StructBuffer<S, B>> List<S>.toBuffer(createCallback: (Int) -> B): B {
    val buffer = createCallback(size)
    for (struct in this) {
        buffer.put(struct)
    }
    buffer.flip()
    return buffer
}

/**
 * Fetches a Vulkan integer buffer using the provided query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the
 * result.
 *
 * Example:
 * ```kotlin
 * val formats = queryVulkanBuffer { countBuffer, resultBuffer ->
 *     vkGetPhysicalDeviceSurfaceFormatsKHR(
 *         physicalDevice.handle,
 *         surface.address.handle,
 *         countBuffer,
 *         resultBuffer
 *     )
 * }
 * ```
 *
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to
 *   `countBuffer`).
 *   2. Second, with an appropriately sized `resultBuffer` to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional buffer to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length
 * arrays.
 *
 * @return An IntBuffer containing the requested data if successful, or `null` if no data was available.
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun MemoryStack.queryVulkanIntBuffer(bufferQuery: MemoryStack.(IntBuffer, IntBuffer?) -> Int): IntBuffer? =
    queryVulkanBuffer(
        bufferInitializer = { mallocInt(it) },
        bufferQuery = bufferQuery,
    )

/**
 * Fetches a Vulkan pointer buffer using the provided query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the
 * result.
 *
 * Example:
 * ```kotlin
 * val extensions = getVulkanPointerBuffer { countBuffer, resultBuffer ->
 *         vkEnumerateInstanceExtensionProperties(
 *             null as String?,
 *             countBuffer,
 *             resultBuffer
 *         )
 *     }
 * ```
 *
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to
 *   `countBuffer`).
 *   2. Second, with an appropriately sized `resultBuffer` to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional `PointerBuffer` to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length
 * arrays.
 *
 * @return A `PointerBuffer` containing the requested data if successful, or `null` if no data was available.
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun MemoryStack.queryVulkanPointerBuffer(
    bufferQuery: MemoryStack.(IntBuffer, PointerBuffer?) -> Int,
): PointerBuffer? = queryVulkanBuffer(
    bufferInitializer = { mallocPointer(it) },
    bufferQuery = bufferQuery,
)

/**
 * Fetches and transforms a Vulkan struct buffer using the provided initializer, query function, and mapper.
 * This function enables transformation from native managed memory structures to JVM managed objects while
 * combining querying Vulkan data with transforming the results into a more convenient form.
 *
 * Example:
 * ```kotlin
 * val deviceProperties = queryAndTransformVulkanBuffer(
 *     bufferInitializer = { VkPhysicalDeviceProperties.malloc(it) },
 *     bufferQuery = { countBuffer, resultBuffer ->
 *         vkEnumeratePhysicalDevices(instance, countBuffer, resultBuffer)
 *     },
 *     bufferMapper = { DeviceProperties(it) }
 * )
 * ```
 *
 * @param T The target type after transformation
 * @param S The type of Vulkan struct being allocated
 * @param B The type of struct buffer being created
 * @param bufferInitializer A function that creates a new struct buffer of the specified size
 *
 * **Note**: The buffer should be allocated using the supplied stack, otherwise it is up to the caller to free the
 * buffer.
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to
 *   `countBuffer`).
 *   2. Second, with a newly allocated result buffer to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional struct buffer to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length
 * arrays.
 * @param bufferMapper A function to transform each struct into the target type
 * @return A list of transformed elements
 * @throws IllegalStateException if any Vulkan operation does not succeed
 */
fun <T, S : Struct<S>, B : StructBuffer<S, B>> queryAndTransformVulkanStructBuffer(
    bufferInitializer: MemoryStack.(Int) -> B,
    bufferQuery: MemoryStack.(IntBuffer, B?) -> Int,
    bufferMapper: (S) -> T,
): List<T> = MemoryStack.stackPush().use { stack ->
    val buffer = stack.queryVulkanBuffer(bufferInitializer, bufferQuery)
    buffer.toList().map { value -> bufferMapper(value) }
}

/**
 * Fetches a Vulkan buffer using the provided buffer initializer and query function.
 * This function internally manages memory allocation and ensures that Vulkan calls are successful before returning the
 * result.
 *
 * Example:
 * ```kotlin
 * val presentModes = getVulkanBuffer(
 *     bufferInitializer = { mallocInt(it) },
 *     bufferQuery = { countBuffer, resultBuffer ->
 *         vkGetPhysicalDeviceSurfacePresentModesKHR(
 *             physicalDevice.handle,
 *             surface.address.handle,
 *             countBuffer,
 *             resultBuffer
 *         )
 *     }
 * )
 * ```
 *
 * @param B The type of buffer being created
 *
 * @param bufferInitializer A function that creates a new buffer of the specified size.
 *
 *   **Note**: The buffer should be allocated using the supplied stack, otherwise it is up to the caller to free the
 *   buffer.
 * @param bufferQuery A lambda that is called **twice**:
 *   1. First, with `resultBuffer = null`, to query Vulkan for the number of available elements (writing the count to
 *   `countBuffer`).
 *   2. Second, with an appropriately sized `resultBuffer` to retrieve the actual data.
 * The lambda takes two parameters:
 *   - `countBuffer`: an `IntBuffer` to store the count of elements queried.
 *   - `resultBuffer`: an optional buffer to hold the actual results, or `null` on the first call.
 * The lambda should return a Vulkan operation result code.
 *
 * This two-call pattern matches the standard Vulkan approach for querying the size and contents of variable-length
 * arrays.
 *
 * @return A buffer containing the requested data if successful
 * @throws IllegalStateException if any Vulkan operation does not succeed.
 */
inline fun <B> MemoryStack.queryVulkanBuffer(
    bufferInitializer: MemoryStack.(Int) -> B,
    bufferQuery: MemoryStack.(IntBuffer, B?) -> Int,
): B {
    val countBuffer = mallocInt(1)
    bufferQuery(countBuffer, null).validateVulkanSuccess()

    if (countBuffer[0] == 0) {
        return bufferInitializer(0)
    }

    val buffer = bufferInitializer(countBuffer[0])
    bufferQuery(countBuffer, buffer).validateVulkanSuccess()
    return buffer
}

inline fun <B> MemoryStack.queryVulkanBufferWithoutSuccessValidation(
    bufferInitializer: MemoryStack.(Int) -> B,
    bufferQuery: MemoryStack.(IntBuffer, B?) -> Unit,
): B {
    val countBuffer = mallocInt(1)
    bufferQuery(countBuffer, null)

    if (countBuffer[0] == 0) {
        return bufferInitializer(0)
    }

    val buffer = bufferInitializer(countBuffer[0])
    bufferQuery(countBuffer, buffer)
    return buffer
}

inline fun <T> IntBuffer?.transform(transform: (Int) -> T?): List<T> {
    if (this == null) {
        return emptyList()
    }

    val list = mutableListOf<T>()
    this.position(0)
    while (this.hasRemaining()) {
        val transformedValue = transform(get())
        if (transformedValue != null) {
            list.add(transformedValue)
        }
    }
    this.position(0)
    return list
}

inline fun <T> PointerBuffer?.transform(transform: (Long) -> T): List<T> {
    if (this == null) {
        return emptyList()
    }

    val list = mutableListOf<T>()
    this.position(0)
    while (this.hasRemaining()) {
        list.add(transform(get()))
    }
    this.position(0)
    return list
}

inline fun <T, S : Struct<S>, B : StructBuffer<S, B>> StructBuffer<S, B>?.transform(transform: (S) -> T): List<T> {
    if (this == null) {
        return emptyList()
    }

    val list = mutableListOf<T>()
    this.position(0)
    while (this.hasRemaining()) {
        list.add(transform(get()))
    }
    this.position(0)
    return list
}
