package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.Mat4
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.scene.BlendMode
import dev.cryptospace.anvil.core.scene.Material
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RenderQueueTest {

    private val queue = RenderQueue<String>(initialCapacity = 2)
    private val material = Material()
    private val transparentMaterial = Material(blendMode = BlendMode.ALPHA_BLEND)

    private fun add(
        mesh: String,
        pipeline: Int = 0,
        materialSlot: Int = 0,
        meshSlot: Int = 0,
        transparent: Boolean = false,
        transform: Mat4 = Mat4.identity,
    ) = queue.add(
        transparent,
        pipeline,
        materialSlot,
        meshSlot,
        mesh,
        if (transparent) transparentMaterial else material,
        transform,
    )

    private fun sortedMeshes() = (0 until queue.size).map(queue::mesh)

    @Test
    fun `sorts opaque before transparent, then by pipeline, material and mesh`() {
        add("transparent", pipeline = 0, transparent = true)
        add("p1-m0", pipeline = 1, materialSlot = 0)
        add("p0-m2-mesh1", pipeline = 0, materialSlot = 2, meshSlot = 1)
        add("p0-m2-mesh0", pipeline = 0, materialSlot = 2, meshSlot = 0)
        add("p0-m1", pipeline = 0, materialSlot = 1)

        queue.sort()

        assertEquals(listOf("p0-m1", "p0-m2-mesh0", "p0-m2-mesh1", "p1-m0", "transparent"), sortedMeshes())
        assertTrue(queue.isTransparent(4))
        assertFalse(queue.isTransparent(0))
    }

    @Test
    fun `keeps insertion order for identical state`() {
        add("first")
        add("second")
        add("third")

        queue.sort()

        assertEquals(listOf("first", "second", "third"), sortedMeshes())
    }

    @Test
    fun `decodes the state of each draw after sorting`() {
        val transform = Mat4.translation(Vec3(1f, 2f, 3f))
        add("b", pipeline = 5, materialSlot = 600, meshSlot = 7000, transform = transform)
        add("a", pipeline = 3, materialSlot = 65_535, meshSlot = 65_535)

        queue.sort()

        assertEquals(3, queue.pipeline(0))
        assertEquals(65_535, queue.materialSlot(0))
        assertEquals(65_535, queue.meshSlot(0))
        assertEquals(5, queue.pipeline(1))
        assertEquals(600, queue.materialSlot(1))
        assertEquals(7000, queue.meshSlot(1))
        assertSame(transform, queue.transform(1))
        assertSame(material, queue.material(1))
    }

    @Test
    fun `state keys group draws that can be batched`() {
        add("a", pipeline = 1, materialSlot = 2, meshSlot = 3)
        add("b", pipeline = 1, materialSlot = 2, meshSlot = 3)
        add("c", pipeline = 1, materialSlot = 2, meshSlot = 4)

        queue.sort()

        assertEquals(queue.stateKey(0), queue.stateKey(1))
        assertNotEquals(queue.stateKey(1), queue.stateKey(2))
    }

    @Test
    fun `grows beyond its initial capacity and can be reused after clearing`() {
        repeat(100) { index -> add("mesh$index", meshSlot = 99 - index) }
        queue.sort()
        assertEquals(100, queue.size)
        assertEquals("mesh99", queue.mesh(0))

        queue.clear()
        assertEquals(0, queue.size)

        add("again")
        queue.sort()
        assertEquals(listOf("again"), sortedMeshes())
    }

    @Test
    fun `rejects values that do not fit into the sort key`() {
        assertFailsWith<IllegalArgumentException> { add("x", pipeline = RenderQueue.MAX_PIPELINES) }
        assertFailsWith<IllegalArgumentException> { add("x", materialSlot = RenderQueue.MAX_MATERIALS) }
        assertFailsWith<IllegalArgumentException> { add("x", meshSlot = -1) }
        assertEquals(0, queue.size)
    }
}
