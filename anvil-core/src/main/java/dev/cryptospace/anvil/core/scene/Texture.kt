package dev.cryptospace.anvil.core.scene

import java.nio.ByteBuffer

data class Texture(
    val imageSize: Int,
    val width: Int,
    val height: Int,
    val imageData: ByteBuffer,
)
