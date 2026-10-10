package dev.cryptospace.anvil.core

import java.lang.management.ManagementFactory

/**
 * Collects CPU-side frame timings and allocation rates, reported periodically when `-Dframe-stats` is set.
 *
 * Only work the engine controls is timed (scene update and command recording), so the numbers are not
 * hidden behind vsync or GPU waits.
 */
class FrameStats {

    private val threadBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean

    private var frames = 0
    private var updateNanos = 0L
    private var recordNanos = 0L
    private var allocatedBytes = 0L
    private var frameStartAllocatedBytes = 0L
    private var lastReportNanos = System.nanoTime()

    val enabled: Boolean = AppConfig.frameStats

    fun addUpdateTime(nanos: Long) {
        updateNanos += nanos
    }

    fun addRecordTime(nanos: Long) {
        recordNanos += nanos
    }

    fun beginFrame() {
        if (enabled) {
            frameStartAllocatedBytes = currentThreadAllocatedBytes()
        }
    }

    fun endFrame() {
        if (!enabled) return

        allocatedBytes += currentThreadAllocatedBytes() - frameStartAllocatedBytes
        frames++

        val now = System.nanoTime()
        if (now - lastReportNanos >= REPORT_INTERVAL_NANOS) {
            logger.info(
                "frames={} update={}ms record={}ms allocated={}KB per frame",
                frames,
                "%.3f".format(updateNanos / frames / 1e6),
                "%.3f".format(recordNanos / frames / 1e6),
                "%.1f".format(allocatedBytes / frames / 1024.0),
            )
            frames = 0
            updateNanos = 0
            recordNanos = 0
            allocatedBytes = 0
            lastReportNanos = now
        }
    }

    private fun currentThreadAllocatedBytes(): Long = threadBean?.currentThreadAllocatedBytes ?: 0L

    companion object {
        private const val REPORT_INTERVAL_NANOS = 2_000_000_000L

        @JvmStatic
        private val logger = logger<FrameStats>()
    }
}
