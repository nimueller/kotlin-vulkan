package dev.cryptospace.anvil.app

import dev.cryptospace.anvil.core.Engine
import dev.cryptospace.anvil.core.input.Key
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.scene.Face
import dev.cryptospace.anvil.core.scene.GameObject
import dev.cryptospace.anvil.core.scene.MeshId
import dev.cryptospace.anvil.core.window.Window
import java.io.InputStream

private const val ROTATION_DEGREES_PER_SECOND: Float = 45f
private const val MAX_ROTATION_IN_DEGREES = 360.0

class ExampleUtils

fun resource(resourcePath: String): InputStream =
    ExampleUtils::class.java.getResourceAsStream(resourcePath) ?: error("Image resource $resourcePath not found")

fun Engine.quadMesh(): MeshId {
    val vertices = arrayOf(
        TexturedVertex3(
            position = Vec3(x = -0.5f, y = -0.5f, z = 0.0f),
            color = Vec3(x = 1.0f, y = 0.0f, z = 0.0f),
            texture = Vec2(x = 0f, y = 1f),
        ),
        TexturedVertex3(
            position = Vec3(x = 0.5f, y = -0.5f, z = 0.0f),
            color = Vec3(x = 0.0f, y = 1.0f, z = 0.0f),
            texture = Vec2(x = 1f, y = 1f),
        ),
        TexturedVertex3(
            position = Vec3(x = 0.5f, y = 0.5f, z = 0.0f),
            color = Vec3(x = 0.0f, y = 0.0f, z = 1.0f),
            texture = Vec2(x = 1f, y = 0f),
        ),
        TexturedVertex3(
            position = Vec3(x = -0.5f, y = 0.5f, z = 0.0f),
            color = Vec3(x = 1.0f, y = 1.0f, z = 1.0f),
            texture = Vec2(x = 0f, y = 0f),
        ),
    )

    val faces = arrayOf(
        Face(indexA = 0U, indexB = 1U, indexC = 2U),
        Face(indexA = 2U, indexB = 3U, indexC = 0U),
    )

    return mesh(TexturedVertex3, vertices, faces)
}

fun GameObject.rotateOnUpdate() {
    var rotationInDegrees = 0.0

    onUpdate = { deltaTime, window ->
        val deltaRotation = deltaTime.seconds * ROTATION_DEGREES_PER_SECOND
        rotationInDegrees += deltaRotation
        rotationInDegrees %= MAX_ROTATION_IN_DEGREES
        transform = Mat4.identity.rotate(Math.toRadians(rotationInDegrees).toFloat(), Vec3(0f, 0f, 1f))

        window.quitIfEscapePressed()
    }
}

fun Window.quitIfEscapePressed() {
    if (isKeyPressed(Key.ESCAPE)) {
        requestClose()
    }
}
