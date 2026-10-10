package dev.cryptospace.anvil.vulkan.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeferredDeletionQueueTest {

    private val deleted = mutableListOf<String>()
    private val queue = DeferredDeletionQueue(framesInFlight = 2)

    @Test
    fun `keeps resources alive while frames in flight may use them`() {
        queue.enqueue(submittedFrames = 10) { deleted += "mesh" }

        queue.flush(currentFrame = 10)
        queue.flush(currentFrame = 11)
        assertTrue(deleted.isEmpty())

        queue.flush(currentFrame = 12)
        assertEquals(listOf("mesh"), deleted)
        assertEquals(0, queue.size)
    }

    @Test
    fun `releases resources in the order they were enqueued`() {
        queue.enqueue(submittedFrames = 1) { deleted += "a" }
        queue.enqueue(submittedFrames = 1) { deleted += "b" }
        queue.enqueue(submittedFrames = 3) { deleted += "c" }

        queue.flush(currentFrame = 4)
        assertEquals(listOf("a", "b"), deleted)
        assertEquals(1, queue.size)

        queue.flush(currentFrame = 5)
        assertEquals(listOf("a", "b", "c"), deleted)
    }

    @Test
    fun `flushAll releases everything`() {
        queue.enqueue(submittedFrames = 100) { deleted += "a" }
        queue.enqueue(submittedFrames = 101) { deleted += "b" }

        queue.flushAll()

        assertEquals(listOf("a", "b"), deleted)
        assertEquals(0, queue.size)
    }

    @Test
    fun `rejects a frame counter going backwards`() {
        queue.enqueue(submittedFrames = 5) { }

        assertFailsWith<IllegalArgumentException> { queue.enqueue(submittedFrames = 4) { } }
    }
}
