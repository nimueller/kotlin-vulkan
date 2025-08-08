package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.input.Input
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeType
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.window.Window
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

private const val DEFAULT_MOVEMENT_SPEED: Float = 1f
private const val DEFAULT_MOUSE_SENSITIVITY: Float = 0.1f
private const val DEFAULT_FOV: Float = 45f
private val DYNAMIC_ASPECT_RATIO: Float? = null
private const val DEFAULT_NEAR_PLANE = 0.1f
private const val DEFAULT_FAR_PLANE = 100f
private const val PITCH_CONSTRAINT_LOWER_BOUND = -89f
private const val PITCH_CONSTRAINT_UPPER_BOUND = 89f

data class Camera(
    private val renderingSystem: RenderingSystem,
    private var viewMatrix: Mat4 = Mat4.identity,
    private var projectionMatrix: Mat4 = Mat4.identity,
) : NativeType {

    // Projection
    var fov: Float = DEFAULT_FOV
        set(value) {
            field = value
            aspectRatio?.let { ratio -> updateProjectionMatrix(ratio) }
        }
    var aspectRatio: Float? = DYNAMIC_ASPECT_RATIO
        set(value) {
            field = value
            field?.let { ratio -> updateProjectionMatrix(ratio) }
        }
    var near: Float = DEFAULT_NEAR_PLANE
        set(value) {
            field = value
            aspectRatio?.let { ratio -> updateProjectionMatrix(ratio) }
        }
    var viewDistance: Float = DEFAULT_FAR_PLANE
        set(value) {
            field = value
            aspectRatio?.let { ratio -> updateProjectionMatrix(ratio) }
        }

    // Movement
    var movementEnabled: Boolean = true
    var movementSpeed: Float = DEFAULT_MOVEMENT_SPEED
    var position: Vec3 = Vec3.zero
        private set
    var front: Vec3 = Vec3.forward
        private set
    var up: Vec3 = Vec3.up
        private set

    // Rotation
    var rotationEnabled: Boolean = false
    var mouseSensitivity: Float = DEFAULT_MOUSE_SENSITIVITY
    var yaw: Float = 0f
        private set
    var pitch: Float = 0f
        private set

    override val byteSize: Int
        get() = Mat4.BYTE_SIZE * 2

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        viewMatrix.toByteBuffer(byteBuffer)
        projectionMatrix.toByteBuffer(byteBuffer)
    }

    fun lookInDirection(yaw: Float, pitch: Float) {
        val newPitch = pitch.coerceIn(PITCH_CONSTRAINT_LOWER_BOUND, PITCH_CONSTRAINT_UPPER_BOUND)

        val yawRadians = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRadians = Math.toRadians(newPitch.toDouble()).toFloat()
        val front = Vec3(
            x = cos(yawRadians) * cos(pitchRadians),
            y = sin(pitchRadians),
            z = sin(yawRadians) * cos(pitchRadians),
        ).normalized()

        lookAt(position, position + front, Vec3.up)
    }

    fun lookInDirection(direction: Vec3, up: Vec3) {
        lookAt(position, position + direction, up)
    }

    fun lookAt(target: Vec3, up: Vec3) {
        lookAt(position, target, up)
    }

    fun lookAt(position: Vec3, target: Vec3, up: Vec3) {
        this.position = position
        this.front = (target - position).normalized()
        this.up = up.normalized()

        pitch = Math.toDegrees(kotlin.math.asin(front.y.toDouble())).toFloat()
        yaw = Math.toDegrees(kotlin.math.atan2(front.z.toDouble(), front.x.toDouble())).toFloat()
        updateViewMatrix()
    }

    private fun updateViewMatrix() {
        viewMatrix = Mat4.lookAt(position, position + front, up)
    }

    fun update(window: Window, renderingContext: RenderingContext, deltaTime: DeltaTime) {
        if (aspectRatio == DYNAMIC_ASPECT_RATIO) {
            val renderCanvasWidth = renderingContext.width.toFloat()
            val renderCanvasHeight = renderingContext.height.toFloat()

            if (renderCanvasWidth > 0f && renderCanvasHeight > 0f) {
                val ratio = renderCanvasWidth / renderCanvasHeight
                updateProjectionMatrix(ratio)
            }
        }

        processKeyboardEvents(window, deltaTime)
        processMouseEvents(window, deltaTime)
    }

    private fun updateProjectionMatrix(ratio: Float) {
        projectionMatrix = renderingSystem.perspective(fov, ratio, near, viewDistance)
    }

    private fun processKeyboardEvents(window: Window, deltaTime: DeltaTime) {
        if (!movementEnabled) {
            return
        }

        val velocity = movementSpeed * deltaTime.seconds.toFloat()

        if (window.isKeyPressed(Input.forwardKey)) position += front * velocity
        if (window.isKeyPressed(Input.backwardKey)) position -= front * velocity
        if (window.isKeyPressed(Input.leftKey)) position += up.cross(front) * velocity
        if (window.isKeyPressed(Input.rightKey)) position -= up.cross(front) * velocity
        if (window.isKeyPressed(Input.upKey)) position += up * velocity
        if (window.isKeyPressed(Input.downKey)) position -= up * velocity
    }

    private fun processMouseEvents(window: Window, deltaTime: DeltaTime) {
        if (!rotationEnabled) {
            return
        }

        val offsetX = window.cursorPosition.x - window.previousCursorPosition.x
        val offsetY = window.cursorPosition.y - window.previousCursorPosition.y

        val newYaw = yaw + offsetX * mouseSensitivity
        val newPitch = pitch - offsetY * mouseSensitivity

        lookInDirection(newYaw, newPitch)
    }
}
