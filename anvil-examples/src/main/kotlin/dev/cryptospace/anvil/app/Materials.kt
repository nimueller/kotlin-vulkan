package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.scene.BlendMode
import dev.cryptospace.anvil.core.scene.CullMode
import dev.cryptospace.anvil.vulkan.vulkan
import kotlin.math.sin

/**
 * Shows the material system: materials that only differ in parameters share a pipeline,
 * render state such as blending gets its own pipeline, and materials can be changed at runtime.
 */
fun main() = vulkan {
    camera.lookAt(Vec3(0f, -2.5f, 2.5f), Vec3.zero, Vec3(0f, 0f, 1f))

    val quad = quadMesh()
    val image = texture(resource("/images/texture.jpg"))

    // These three only differ in parameters, so they are drawn with the same pipeline
    val textured = material { texture = image }
    val tinted = material {
        texture = image
        properties.albedo = Vec4(0.4f, 0.8f, 1f, 1f)
    }
    val pulsing = material { }

    // Different render state, so this one gets its own pipeline and is drawn after all opaque objects
    val glass = material {
        properties.albedo = Vec4(1f, 1f, 1f, 0.35f)
        blendMode = BlendMode.ALPHA_BLEND
        cullMode = CullMode.NONE
        depthWrite = false
    }

    val font = font(resource("/fonts/MorrisRoman-Black.ttf"), pixelHeight = 28f)
    hud.label("Textured / tinted / animated, with a glass pane in front", Vec2(16f, 16f), font)

    var elapsedSeconds = 0.0

    scene {
        gameObject {
            transform.position = Vec3(-1.1f, 0f, 0f)
            renderComponent {
                meshId = quad
                materialId = textured
            }
        }

        gameObject {
            renderComponent {
                meshId = quad
                materialId = tinted
            }
        }

        gameObject {
            transform.position = Vec3(1.1f, 0f, 0f)
            renderComponent {
                meshId = quad
                materialId = pulsing
            }

            onUpdate { deltaTime, window ->
                elapsedSeconds += deltaTime.seconds
                val brightness = 0.5f + 0.5f * sin(elapsedSeconds * 2.0).toFloat()
                updateMaterial(pulsing) {
                    properties.albedo = Vec4(brightness, brightness, brightness, 1f)
                }

                window.quitIfEscapePressed()
            }
        }

        gameObject {
            transform.position = Vec3(-0.55f, -0.3f, 0.3f)
            transform.scale = Vec3(2f, 1f, 1f)
            renderComponent {
                meshId = quad
                materialId = glass
            }
        }
    }
}
