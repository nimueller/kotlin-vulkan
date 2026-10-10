package dev.cryptospace.anvil.core.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegistryTest {

    @Test
    fun `add assigns sequential indices and get returns the objects`() {
        val registry = Registry<String>()

        val a = registry.add("a")
        val b = registry.add("b")

        assertEquals(0, a)
        assertEquals(1, b)
        assertEquals("a", registry[a])
        assertEquals("b", registry[b])
        assertEquals(2, registry.size)
    }

    @Test
    fun `grows beyond its initial capacity`() {
        val registry = Registry<Int>(initial = 2)

        val indices = (0 until 100).map(registry::add)

        assertEquals((0 until 100).toList(), indices.map { index -> registry[index] })
        assertEquals(100, registry.size)
    }

    @Test
    fun `remove frees the slot for reuse`() {
        val registry = Registry<String>()
        registry.add("a")
        val b = registry.add("b")
        registry.add("c")

        assertEquals("b", registry.remove(b))
        assertNull(registry[b])
        assertFalse(b in registry)
        assertEquals(2, registry.size)

        assertEquals(b, registry.add("d"))
        assertEquals("d", registry[b])
        assertEquals(3, registry.size)
    }

    @Test
    fun `removing an empty slot twice does not hand out the index twice`() {
        val registry = Registry<String>()
        val a = registry.add("a")

        registry.remove(a)
        assertNull(registry.remove(a))

        val first = registry.add("x")
        val second = registry.add("y")
        assertTrue(first != second)
        assertEquals(2, registry.size)
    }

    @Test
    fun `get and remove return null for invalid indices`() {
        val registry = Registry<String>()

        assertNull(registry[-1])
        assertNull(registry[1000])
        assertNull(registry.remove(1000))
        assertEquals(0, registry.size)
    }

    @Test
    fun `set replaces existing objects only`() {
        val registry = Registry<String>()
        val a = registry.add("a")

        registry[a] = "b"

        assertEquals("b", registry[a])
        assertFailsWith<IllegalArgumentException> { registry[42] = "c" }
    }

    @Test
    fun `getAll skips removed objects`() {
        val registry = Registry<String>()
        registry.add("a")
        val b = registry.add("b")
        registry.add("c")
        registry.remove(b)

        assertEquals(listOf("a", "c"), registry.getAll())
    }
}
