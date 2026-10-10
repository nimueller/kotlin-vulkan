package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.DeltaTime
import dev.cryptospace.anvil.core.window.Window
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SceneTest {

    private val window: Window = mock()

    @Test
    fun `updates every object once per frame`() {
        val scene = Scene()
        val updates = mutableListOf<String>()
        scene.gameObject { onUpdate = { _, _ -> updates += "a" } }
        scene.gameObject { }
        scene.gameObject { onUpdate = { _, _ -> updates += "c" } }

        scene.update(DeltaTime(0.016), window)

        assertEquals(listOf("a", "c"), updates)
    }

    @Test
    fun `objects may remove themselves during the update`() {
        val scene = Scene()
        val updated = mutableListOf<Int>()
        lateinit var first: GameObject
        first = scene.gameObject {
            onUpdate = { _, _ ->
                updated += 1
                scene.remove(first)
            }
        }
        scene.gameObject { onUpdate = { _, _ -> updated += 2 } }

        scene.update(DeltaTime(0.016), window)

        assertEquals(1, scene.gameObjects.size)
        assertEquals(1, updated.first())
    }

    @Test
    fun `objects added during the update are updated from the next frame on`() {
        val scene = Scene()
        var spawnedUpdates = 0
        scene.gameObject {
            onUpdate = { _, _ ->
                if (scene.gameObjects.size == 1) {
                    scene.gameObject { onUpdate = { _, _ -> spawnedUpdates++ } }
                }
            }
        }

        scene.update(DeltaTime(0.016), window)
        assertEquals(0, spawnedUpdates)

        scene.update(DeltaTime(0.016), window)
        assertEquals(1, spawnedUpdates)
    }

    @Test
    fun `game objects use identity equality`() {
        // Mutable objects must not change their hash while stored in sets or maps
        val a = GameObject()
        val b = GameObject()

        assertNotEquals(a, b)
        val hash = a.hashCode()
        a.transform.position = dev.cryptospace.anvil.core.math.Vec3(1f, 2f, 3f)
        a.visible = false
        assertEquals(hash, a.hashCode())
    }
}
