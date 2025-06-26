package dev.cryptospace.anvil.vulkan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
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
        override fun create(
            address: Long,
            container: ByteBuffer?,
        ): DummyStruct = DummyStruct(address, container)

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
    fun `getVulkanPointerBuffer returns null if count is zero`() {
        val buffer =
            getVulkanPointerBuffer { countBuffer, resultBuffer ->
                countBuffer.put(0, 0)
                VK_SUCCESS
            }
        assert(buffer == null)
    }

    @Test
    fun `getVulkanPointerBuffer returns filled PointerBuffer on success`() {
        val dummyAddresses = longArrayOf(0x1L, 0x2L, 0x3L)
        val buffer =
            getVulkanPointerBuffer { countBuffer, resultBuffer ->
                if (resultBuffer == null) {
                    countBuffer.put(0, dummyAddresses.size)
                } else {
                    for (i in dummyAddresses.indices) {
                        resultBuffer.put(i, dummyAddresses[i])
                    }
                }
                VK_SUCCESS
            }

        assert(buffer?.limit() == 3)
        assert(buffer?.get(0) == dummyAddresses[0])
        assert(buffer?.get(1) == dummyAddresses[1])
        assert(buffer?.get(2) == dummyAddresses[2])
    }

    @Test
    fun `getVulkanPointerBuffer throws on error code`() {
        assertThrows(IllegalStateException::class.java) {
            getVulkanPointerBuffer { _, _ -> VK_ERROR_UNKNOWN }
        }
    }

    @Test
    fun `getVulkanBuffer returns null if count is zero`() {
        val buffer =
            getVulkanBuffer { countBuffer, resultBuffer ->
                countBuffer.put(0, 0)
                VK_SUCCESS
            }
        assert(buffer == null)
    }

    @Test
    fun `getVulkanBuffer returns filled IntBuffer on success`() {
        val data = intArrayOf(2, 4, 6, 8)
        val buffer =
            getVulkanBuffer { countBuffer, resultBuffer ->
                if (resultBuffer == null) {
                    countBuffer.put(0, data.size)
                } else {
                    for (i in data.indices) resultBuffer.put(i, data[i])
                }
                VK_SUCCESS
            }

        assert(buffer?.limit() == data.size)
        assertArrayEquals(data, IntArray(buffer!!.remaining()) { buffer[it] })
    }

    @Test
    fun `getVulkanBuffer throws on error code`() {
        assertThrows(IllegalStateException::class.java) {
            getVulkanBuffer { _, _ -> VK_ERROR_UNKNOWN }
        }
    }

    @Test
    fun `getVulkanBuffer for StructBuffer returns empty buffer if count is zero`() {
        val buffer =
            getVulkanBuffer(
                bufferInitializer = { DummyStruct.Buffer(ByteBuffer.allocateDirect(0)) },
                bufferQuery = { countBuffer, _ ->
                    countBuffer.put(0, 0)
                    VK_SUCCESS
                },
            )
        assert(buffer.remaining() == 0)
    }

    @Test
    fun `getVulkanBuffer for StructBuffer returns filled buffer`() {
        val buffer =
            getVulkanBuffer(
                bufferInitializer = { size -> DummyStruct.Buffer(ByteBuffer.allocateDirect(size)) },
                bufferQuery = { countBuffer, resultBuffer ->
                    if (resultBuffer == null) {
                        countBuffer.put(0, 2)
                    }

                    VK_SUCCESS
                },
            )
        assert(buffer.remaining() == 2)
    }

    @Test
    fun `getVulkanBuffer for StructBuffer throws on error code`() {
        assertThrows(IllegalStateException::class.java) {
            getVulkanBuffer(
                bufferInitializer = { DummyStruct.Buffer(ByteBuffer.allocateDirect(0)) },
                bufferQuery = { _, _ -> VK_ERROR_UNKNOWN },
            )
        }
    }
}
