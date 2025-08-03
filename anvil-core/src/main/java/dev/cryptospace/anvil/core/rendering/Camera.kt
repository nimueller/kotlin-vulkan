package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.input.Input
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeType
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.window.Window
import java.nio.ByteBuffer

private const val DEFAULT_MOVEMENT_SPEED: Float = 1f
private const val DEFAULT_MOUSE_SENSITIVITY: Float = 0.5f
private const val DEFAULT_FOV: Float = 45f
private val DYNAMIC_ASPECT_RATIO: Float? = null
private const val DEFAULT_NEAR_PLANE = 0.1f
private const val DEFAULT_FAR_PLANE = 100f

data class Camera(
    private val renderingSystem: RenderingSystem,
    private var viewMatrix: Mat4 = Mat4.identity,
    private var projectionMatrix: Mat4 = Mat4.identity,
) : NativeType {

    var position: Vec3 = Vec3.zero
        set(value) {
            field = value
            updateViewMatrix()
        }
    var front: Vec3 = Vec3.forward
        set(value) {
            field = value.normalized()
            updateViewMatrix()
        }
    var up: Vec3 = Vec3.up
        set(value) {
            field = value
            updateViewMatrix()
        }
    var movementEnabled: Boolean = true
    var movementSpeed: Float = DEFAULT_MOVEMENT_SPEED
    var mouseSensitivity: Float = DEFAULT_MOUSE_SENSITIVITY
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

    override val byteSize: Int
        get() = Mat4.BYTE_SIZE * 2

    override fun toByteBuffer(byteBuffer: ByteBuffer) {
        viewMatrix.toByteBuffer(byteBuffer)
        projectionMatrix.toByteBuffer(byteBuffer)
    }

    fun lookAt(target: Vec3, up: Vec3): Mat4 = lookAt(position, target, up)

    fun lookAt(position: Vec3, target: Vec3, up: Vec3): Mat4 {
        this.position = position
        this.front = (target - position).normalized()
        this.up = up.normalized()
        return viewMatrix
    }

    private fun updateViewMatrix() {
        viewMatrix = Mat4.lookAt(position, position + front, up)
    }

    fun update(window: Window, renderingContext: RenderingContext, deltaTime: DeltaTime) {
        if (aspectRatio == DYNAMIC_ASPECT_RATIO) {
            val renderCanvasWidth = renderingContext.width.toFloat()
            val renderCanvasHeight = renderingContext.height.toFloat()
            if (renderCanvasWidth > 0f || renderCanvasHeight > 0f) {
                val ratio = aspectRatio ?: (renderCanvasWidth / renderCanvasHeight)
                updateProjectionMatrix(ratio)
            }
        }

        if (!movementEnabled) {
            return
        }

        processKeyboardEvents(window, deltaTime)
    }

    private fun updateProjectionMatrix(ratio: Float) {
        projectionMatrix = renderingSystem.perspective(fov, ratio, near, viewDistance)
    }

    private fun processKeyboardEvents(window: Window, deltaTime: DeltaTime) {
        val velocity = movementSpeed * deltaTime.seconds.toFloat()

        if (window.isKeyPressed(Input.forwardKey)) position += front * velocity
        if (window.isKeyPressed(Input.backwardKey)) position -= front * velocity
        if (window.isKeyPressed(Input.leftKey)) position += up.cross(front) * velocity
        if (window.isKeyPressed(Input.rightKey)) position -= up.cross(front) * velocity
        if (window.isKeyPressed(Input.upKey)) position += up * velocity
        if (window.isKeyPressed(Input.downKey)) position -= up * velocity
    }
}
