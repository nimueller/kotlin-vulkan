package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.vulkan

fun main() = vulkan {
    camera.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))

    scene {
        gameObject {
            renderComponent {
                meshId = mesh(resource("/models/viking-room.obj")).first()
                materialId = material {
                    texture = texture(resource("/textures/viking-room.png"))
                }
            }

            rotateOnUpdate()
        }
    }
}
