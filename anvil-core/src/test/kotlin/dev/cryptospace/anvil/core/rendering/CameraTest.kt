package dev.cryptospace.anvil.core.rendering

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.FakeRenderingSystem
import dev.cryptospace.anvil.core.window.Window
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class CameraTest {

    private val renderingSystem = FakeRenderingSystem()
    private val window: Window = mock()

    private fun context(width: Int, height: Int) = object : RenderingContext {
        override val width = width
        override val height = height
    }

    private fun Camera.updateFor(width: Int, height: Int) = update(window, context(width, height), DeltaTime(0.0))

    @Test
    fun `projection is only rebuilt when the aspect ratio changes`() {
        val camera = Camera(renderingSystem).apply { movementEnabled = false }

        camera.updateFor(1920, 1080)
        val projection = camera.projection
        camera.updateFor(1920, 1080)

        assertSame(projection, camera.projection)

        camera.updateFor(1080, 1920)
        assertNotEquals(projection, camera.projection)
    }

    @Test
    fun `changing the field of view applies with a dynamic aspect ratio`() {
        val camera = Camera(renderingSystem).apply { movementEnabled = false }
        camera.updateFor(1920, 1080)
        val projection = camera.projection

        camera.fov = 90f
        camera.updateFor(1920, 1080)

        assertNotEquals(projection, camera.projection)
        assertEquals(
            renderingSystem.perspective(90f, 1920f / 1080f, camera.near, camera.viewDistance),
            camera.projection,
        )
    }
}
