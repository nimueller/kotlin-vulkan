package dev.cryptospace.anvil.vulkan

import dev.cryptospace.anvil.vulkan.utils.VulkanUnknownException
import dev.cryptospace.anvil.vulkan.utils.queryVulkanBuffer
import dev.cryptospace.anvil.vulkan.utils.queryVulkanIntBuffer
import dev.cryptospace.anvil.vulkan.utils.queryVulkanPointerBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Struct
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.VK10.VK_ERROR_UNKNOWN
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import java.nio.ByteBuffer
import kotlin.test.Test

class VulkanExtensionsTest {
    class DummyStruct(
        address: Long,
        container: ByteBuffer?,
    ) : Struct<DummyStruct>(address, container) {

        override fun create(address: Long, container: ByteBuffer?): DummyStruct = DummyStruct(address, container)

        override fun sizeof(): Int = 0

        class Buffer(
            container: ByteBuffer,
        ) : StructBuffer<DummyStruct, Buffer>(container, container.remaining()) {

            override fun getElementFactory(): DummyStruct = DummyStruct(0, null)

            override fun self(): Buffer = this

            override fun create(
                address: Long,
                container: ByteBuffer?,
                mark: Int,
                position: Int,
                limit: Int,
                capacity: Int,
            ): Buffer {
                TODO("Not yet implemented")
            }
        }
    }

    @Test
    fun `getVulkanPointerBuffer returns empty buffer if count is zero`() {
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanPointerBuffer { countBuffer, resultBuffer ->
                countBuffer.put(0, 0)
                VK_SUCCESS
            }
        }
        assert(buffer != null)
        assert(buffer?.limit() == 0)
    }

    @Test
    fun `getVulkanPointerBuffer returns filled PointerBuffer on success`() {
        val dummyAddresses = longArrayOf(0x1L, 0x2L, 0x3L)
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanPointerBuffer { countBuffer, resultBuffer ->
                if (resultBuffer == null) {
                    countBuffer.put(0, dummyAddresses.size)
                } else {
                    for (i in dummyAddresses.indices) {
                        resultBuffer.put(i, dummyAddresses[i])
                    }
                }
                VK_SUCCESS
            }
        }

        assert(buffer?.limit() == 3)
        assert(buffer?.get(0) == dummyAddresses[0])
        assert(buffer?.get(1) == dummyAddresses[1])
        assert(buffer?.get(2) == dummyAddresses[2])
    }

    @Test
    fun `getVulkanPointerBuffer should throw on error code`() {
        assertThrows(VulkanUnknownException::class.java) {
            MemoryStack.stackPush().use { stack ->
                stack.queryVulkanPointerBuffer { _, _ -> VK_ERROR_UNKNOWN }
            }
        }
    }

    @Test
    fun `getVulkanBuffer returns empty buffer if count is zero`() {
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanIntBuffer { countBuffer, resultBuffer ->
                countBuffer.put(0, 0)
                VK_SUCCESS
            }
        }
        assert(buffer != null)
        assert(buffer?.limit() == 0)
    }

    @Test
    fun `getVulkanBuffer returns filled IntBuffer on success`() {
        val data = intArrayOf(2, 4, 6, 8)
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanIntBuffer { countBuffer, resultBuffer ->
                if (resultBuffer == null) {
                    countBuffer.put(0, data.size)
                } else {
                    for (i in data.indices) resultBuffer.put(i, data[i])
                }
                VK_SUCCESS
            }
        }

        assert(buffer?.limit() == data.size)
        assertArrayEquals(data, IntArray(buffer!!.remaining()) { buffer[it] })
    }

    @Test
    fun `getVulkanBuffer should throw on error code`() {
        assertThrows(VulkanUnknownException::class.java) {
            MemoryStack.stackPush().use { stack ->
                stack.queryVulkanIntBuffer { _, _ -> VK_ERROR_UNKNOWN }
            }
        }
    }

    @Test
    fun `getVulkanBuffer for StructBuffer should return empty buffer if count is zero`() {
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBuffer(
                bufferInitializer = { DummyStruct.Buffer(ByteBuffer.allocateDirect(0)) },
            ) { countBuffer, _ ->
                countBuffer.put(0, 0)
                VK_SUCCESS
            }
        }
        assert(buffer.remaining() == 0)
    }

    @Test
    fun `getVulkanBuffer for StructBuffer should return filled buffer`() {
        val buffer = MemoryStack.stackPush().use { stack ->
            stack.queryVulkanBuffer(
                bufferInitializer = { size -> DummyStruct.Buffer(ByteBuffer.allocateDirect(size)) },
            ) { countBuffer, resultBuffer ->
                if (resultBuffer == null) {
                    countBuffer.put(0, 2)
                }

                VK_SUCCESS
            }
        }
        assert(buffer.remaining() == 2)
    }

    @Test
    fun `getVulkanBuffer for StructBuffer should throw on error code`() {
        assertThrows(VulkanUnknownException::class.java) {
            MemoryStack.stackPush().use { stack ->
                stack.queryVulkanBuffer(
                    bufferInitializer = { DummyStruct.Buffer(ByteBuffer.allocateDirect(0)) },
                ) { _, _ -> VK_ERROR_UNKNOWN }
            }
        }
    }
}
