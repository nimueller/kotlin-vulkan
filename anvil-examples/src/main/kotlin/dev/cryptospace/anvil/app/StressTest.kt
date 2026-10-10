package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.scene.BlendMode
import dev.cryptospace.anvil.vulkan.vulkan

private const val GRID_SIZE = 100
private const val SPACING = 0.6f

/**
 * Many small objects with a handful of materials, to measure CPU overhead per object.
 * Run with `-Dframe-stats` to log timings.
 */
fun main() = vulkan {
    val extent = GRID_SIZE * SPACING
    camera.lookAt(Vec3(0f, -extent * 0.6f, extent * 0.9f), Vec3.zero, Vec3(0f, 0f, 1f))

    val quad = quadMesh()
    val image = texture(resource("/images/texture.jpg"))
    val materials = listOf(
        material { texture = image },
        material {
            texture = image
            properties.albedo = Vec4(0.4f, 0.8f, 1f, 1f)
        },
        material { },
        material {
            properties.albedo = Vec4(1f, 1f, 1f, 0.5f)
            blendMode = BlendMode.ALPHA_BLEND
            depthWrite = false
        },
    )

    scene {
        for (x in 0 until GRID_SIZE) {
            for (y in 0 until GRID_SIZE) {
                gameObject {
                    transform.position = Vec3((x - GRID_SIZE / 2) * SPACING, (y - GRID_SIZE / 2) * SPACING, 0f)
                    transform.scale = Vec3(0.5f, 0.5f, 0.5f)
                    renderComponent {
                        meshId = quad
                        materialId = materials[(x + y) % materials.size]
                    }
                    rotateOnUpdate()
                }
            }
        }
    }
}
