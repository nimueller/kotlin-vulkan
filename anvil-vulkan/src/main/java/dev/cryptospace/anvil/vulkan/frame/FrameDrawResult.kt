package dev.cryptospace.anvil.vulkan.frame

enum class FrameDrawResult(
    /** Whether command buffers were submitted to the GPU, i.e. the frame slot is now in use. */
    val submitted: Boolean,
    /** Whether the swap chain no longer matches the surface and must be recreated. */
    val swapChainOutdated: Boolean,
) {
    /** The frame was rendered and presented. */
    SUCCESS(submitted = true, swapChainOutdated = false),

    /** The frame was rendered, but presenting reported that the swap chain is out of date or suboptimal. */
    PRESENTED_SWAP_CHAIN_OUTDATED(submitted = true, swapChainOutdated = true),

    /** No swap chain image could be acquired, nothing was rendered. */
    SKIPPED_SWAP_CHAIN_OUTDATED(submitted = false, swapChainOutdated = true),
}
