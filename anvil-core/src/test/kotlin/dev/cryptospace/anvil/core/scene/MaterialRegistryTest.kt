package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.math.Vec4
import dev.cryptospace.anvil.core.shader.ShaderId
import dev.cryptospace.anvil.core.shader.ShaderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class MaterialRegistryTest {

    private val red = Vec4(1f, 0f, 0f, 1f)
    private val blue = Vec4(0f, 0f, 1f, 1f)

    @Test
    fun `stores a snapshot that is unaffected by later mutation`() {
        val registry = MaterialRegistry()
        val material = Material(properties = MaterialProperties(albedo = red))

        val materialId = registry.add(material)
        material.properties.albedo = blue
        material.shaders[ShaderType.VERTEX] = ShaderId(7)
        material.blendMode = BlendMode.ADDITIVE

        val stored = registry.resolve(materialId)
        assertEquals(red, stored.properties.albedo)
        assertEquals(emptyMap(), stored.shaders)
        assertEquals(BlendMode.OPAQUE, stored.blendMode)
    }

    @Test
    fun `get returns a copy so callers cannot modify the stored material`() {
        val registry = MaterialRegistry()
        val materialId = registry.add(Material(properties = MaterialProperties(albedo = red)))

        val copy = registry[materialId]
        copy.properties.albedo = blue

        assertNotSame(copy, registry.resolve(materialId))
        assertEquals(red, registry.resolve(materialId).properties.albedo)
    }

    @Test
    fun `update replaces the stored material`() {
        val registry = MaterialRegistry()
        val materialId = registry.add(Material())

        registry.update(materialId, Material(properties = MaterialProperties(albedo = blue), depthWrite = false))

        assertEquals(blue, registry.resolve(materialId).properties.albedo)
        assertFalse(registry.resolve(materialId).depthWrite)
    }

    @Test
    fun `remove makes the material unknown`() {
        val registry = MaterialRegistry()
        val materialId = registry.add(Material())

        registry.remove(materialId)

        assertFalse(materialId in registry)
        assertNull(registry.resolveOrNull(materialId))
        assertEquals(0, registry.size)
        assertFailsWith<IllegalArgumentException> { registry.resolve(materialId) }
        assertFailsWith<IllegalArgumentException> { registry.update(materialId, Material()) }
        assertFailsWith<IllegalArgumentException> { registry.remove(materialId) }
    }

    @Test
    fun `unknown ids are rejected`() {
        val registry = MaterialRegistry()

        assertFailsWith<IllegalArgumentException> { registry[MaterialId(3)] }
    }
}
