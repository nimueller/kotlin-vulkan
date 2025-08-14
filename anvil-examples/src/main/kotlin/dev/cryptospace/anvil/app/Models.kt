package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.vulkan
import java.io.InputStream

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f
private const val MAX_ROTATION_IN_DEGREES = 360.0

private fun resource(resourcePath: String): InputStream =
    TexturedQuad::class.java.getResourceAsStream(resourcePath) ?: error("Image resource $resourcePath not found")

fun main() = vulkan {
    val vikingRoomTexture = material(resource("/textures/viking-room.png"))

    camera.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))
    camera.movementEnabled = false
    camera.rotationEnabled = false

    scene {
        gameObject {
            renderComponent {
                meshId = mesh(resource("/models/viking-room.obj")).first()
            }

            var rotationInDegrees = 0.0
            onUpdate { deltaTime, window ->
                val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
                rotationInDegrees += deltaRotation
                rotationInDegrees %= MAX_ROTATION_IN_DEGREES
                transform = Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))

                if (window.isKeyPressed(Key.ESCAPE)) {
                    window.requestClose()
                }
            }
        }
    }
}
