package dev.cryptospace.anvil.vulkan.surface

import org.lwjgl.vulkan.KHRSurface

/**
 * Represents various presentation modes available in Vulkan for displaying frames on a surface.
 * Each mode defines how the application's rendered images are queued and presented to the display.
 *
 * @property vulkanValue The native Vulkan value corresponding to this present mode
 */
enum class SurfacePresentMode(
    val vulkanValue: Int,
) {

    /**
     * Images submitted by your application are transferred to the screen right away,
     * which may result in tearing. No queuing mechanism is used.
     */
    IMMEDIATE(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),

    /**
     * Similar to triple buffering - when the queue is full, newer images replace
     * the already queued ones instead of blocking. This allows rendering frames as fast as
     * possible while avoiding tearing, resulting in lower latency than standard vertical sync.
     */
    MAILBOX(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR),

    /**
     * Uses a FIFO queue where the display takes an image from the front when refreshed
     * and the program inserts rendered images at the back. If the queue is full, the program must wait.
     * Most similar to traditional vertical sync found in modern games. Display refresh occurs
     * at "vertical blank".
     *
     * **Notes:**
     * - This mode is guaranteed to always be available.
     * - Least energy consumption, consider for mobile devices.
     */
    FIFO(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),

    /**
     * Variation of FIFO mode that allows tearing if the application is late and the queue was empty
     * at the last vertical blank. Instead of waiting for the next vertical blank, the image
     * is transferred immediately when it arrives.
     */
    FIFO_RELAXED(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR),
    ;

    companion object {

        /** Converts a Vulkan native present mode value to its corresponding enum value */
        fun fromVulkanValue(value: Int): SurfacePresentMode? =
            SurfacePresentMode.entries.firstOrNull { it.vulkanValue == value }
    }
}
