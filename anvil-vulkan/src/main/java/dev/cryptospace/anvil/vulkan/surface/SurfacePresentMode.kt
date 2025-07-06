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

    /** Images submitted are transferred to screen right away, which may result in tearing */
    IMMEDIATE(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),

    /** Triple buffering mode - replaces images in the queue with newer ones */
    MAILBOX(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR),

    /** FIFO queue for display - similar to VSync */
    FIFO(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),

    /** Like FIFO but allows tearing if the application is late */
    FIFO_RELAXED(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR),
    ;

    companion object {

        /** Converts a Vulkan native present mode value to its corresponding enum value */
        fun fromVulkanValue(value: Int): SurfacePresentMode? =
            SurfacePresentMode.entries.firstOrNull { it.vulkanValue == value }
    }
}
