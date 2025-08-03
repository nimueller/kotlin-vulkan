package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.input.Input
import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.NativeType
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.window.Window
import java.nio.ByteBuffer

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

    var moveSpeed = 1f

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

    fun update(window: Window, deltaTime: DeltaTime) {
        processKeyboardEvents(window, deltaTime)
    }

    private fun processKeyboardEvents(window: Window, deltaTime: DeltaTime) {
        val velocity = moveSpeed * deltaTime.seconds.toFloat()

        if (window.isKeyPressed(Input.forwardKey)) position += front * velocity
        if (window.isKeyPressed(Input.backwardKey)) position -= front * velocity
        if (window.isKeyPressed(Input.leftKey)) position += up.cross(front) * velocity
        if (window.isKeyPressed(Input.rightKey)) position -= up.cross(front) * velocity
        if (window.isKeyPressed(Input.upKey)) position += up * velocity
        if (window.isKeyPressed(Input.downKey)) position -= up * velocity
    }
}
