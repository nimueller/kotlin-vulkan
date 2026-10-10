package dev.cryptospace.anvil.vulkan.frame

/**
 * Postpones destroying GPU resources until no frame in flight can still be using them.
 *
 * Frames are numbered by how many frames were submitted before them. A resource released after `n` submissions may
 * still be read by frames up to `n - 1`. Recording frame `f` starts by waiting on the fence of frame
 * `f - framesInFlight`, and a fence also covers all earlier submissions, so the resource is safe to destroy while
 * recording frame `n - 1 + framesInFlight`. This queue waits until frame `n + framesInFlight`, one frame longer
 * than necessary, to leave room for error.
 *
 * @param framesInFlight Number of frames the CPU may record ahead of the GPU
 */
class DeferredDeletionQueue(
    private val framesInFlight: Int,
) {

    private class Entry(
        val releasedAfterFrame: Long,
        val action: () -> Unit,
    )

    private val pending = ArrayDeque<Entry>()

    val size: Int
        get() = pending.size

    /**
     * Schedules [action] to run once all frames submitted so far have completed.
     *
     * @param submittedFrames Number of frames submitted to the GPU at the time of the call
     */
    fun enqueue(submittedFrames: Long, action: () -> Unit) {
        val last = pending.lastOrNull()
        require(last == null || last.releasedAfterFrame <= submittedFrames) {
            "Frame counter went backwards: $submittedFrames < ${last?.releasedAfterFrame}"
        }
        pending.addLast(Entry(submittedFrames, action))
    }

    /**
     * Runs all actions whose resources can no longer be in use.
     * Must only be called after waiting on the in-flight fence of the frame currently being recorded.
     *
     * @param currentFrame Number of the frame currently being recorded (equals the number of frames submitted so far)
     */
    fun flush(currentFrame: Long) {
        while (pending.isNotEmpty() && pending.first().releasedAfterFrame + framesInFlight <= currentFrame) {
            pending.removeFirst().action()
        }
    }

    /**
     * Runs all pending actions. Only safe once the device is idle.
     */
    fun flushAll() {
        while (pending.isNotEmpty()) {
            pending.removeFirst().action()
        }
    }
}
