package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.vulkan.vulkan

private const val FPS_UPDATE_INTERVAL_SECONDS = 0.5

fun main() = vulkan {
    camera.lookAt(Vec3(2f, 2f, 2f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 1f))

    val font = font(resource("/fonts/MorrisRoman-Black.ttf"), pixelHeight = 32f)
    val fpsLabel = hud.label("FPS: -", Vec2(16f, 16f), font)
    var framesSinceUpdate = 0
    var secondsSinceUpdate = 0.0

    scene {
        gameObject {
            transform = Mat4.identity.translate(Vec3(0f, 0f, -2f))

            renderComponent {
                meshId = quadMesh()
                materialId = material {
                    texture = texture(resource("/images/texture.jpg"))
                }
            }
        }

        gameObject {
            renderComponent {
                meshId = mesh(resource("/models/viking-room.obj")).first()
                materialId = material {
                    texture = texture(resource("/textures/viking-room.png"))
                }
            }

            onUpdate = { deltaTime, window ->
                val rotationInDegrees = deltaTime.seconds * 45f
                transform = transform.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))

                // Average over an interval, so the label is readable and its mesh is not rebuilt every frame
                framesSinceUpdate++
                secondsSinceUpdate += deltaTime.seconds
                if (secondsSinceUpdate >= FPS_UPDATE_INTERVAL_SECONDS) {
                    hud.updateLabel(fpsLabel, "FPS: ${(framesSinceUpdate / secondsSinceUpdate).toInt()}")
                    framesSinceUpdate = 0
                    secondsSinceUpdate = 0.0
                }

                window.quitIfEscapePressed()
            }
        }
    }
}
