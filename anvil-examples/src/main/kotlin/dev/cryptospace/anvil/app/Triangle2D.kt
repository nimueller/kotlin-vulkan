package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.scene.Face
import dev.cryptospace.anvil.vulkan.vulkan

private val vertices = arrayOf(
    TexturedVertex2(position = Vec2(x = 0f, y = -0.5f), color = Vec3(x = 0.0f, y = 1.0f, z = 0.0f)),
    TexturedVertex2(position = Vec2(x = -0.5f, y = 0.5f), color = Vec3(x = 1.0f, y = 0.0f, z = 0.0f)),
    TexturedVertex2(position = Vec2(x = 0.5f, y = 0.5f), color = Vec3(x = 0.0f, y = 0.0f, z = 1.0f)),
)

private val faces = arrayOf(
    Face(0U, 1U, 2U),
)

fun main() = vulkan {
    scene {
        gameObject {
            renderComponent {
                meshId = mesh(TexturedVertex2, vertices, faces)
            }
            onUpdate { _, window ->
                window.quitIfEscapePressed()
            }
        }
    }
}
