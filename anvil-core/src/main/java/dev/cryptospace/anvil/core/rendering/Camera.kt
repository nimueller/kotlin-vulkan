package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.DeltaTime
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
    private var viewMatrix: Mat4 = Mat4.identity,
    var projectionMatrix: Mat4 = Mat4.identity,
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
    var aspectRatio: Float? = DYNAMIC_ASPECT_RATIO
    var near: Float = DEFAULT_NEAR_PLANE
    var viewDistance: Float = DEFAULT_FAR_PLANE

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
            updateProjectionMatrix(renderingContext)
        }

        if (!movementEnabled) {
            return
        }

        processKeyboardEvents(window, deltaTime)
    }

    private fun updateProjectionMatrix(renderingContext: RenderingContext) {
        val ratio = aspectRatio ?: (renderingContext.width.toFloat() / renderingContext.height.toFloat())
        projectionMatrix = Mat4.perspectiveVulkan(fov, ratio, near, viewDistance)
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
