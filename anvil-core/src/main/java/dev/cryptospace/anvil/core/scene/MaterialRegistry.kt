package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.collections.Registry

/**
 * Backend-independent bookkeeping for uploaded materials.
 *
 * Stores defensive snapshots, so callers can keep mutating their [Material] instances without affecting
 * what is rendered until they explicitly call [update].
 */
class MaterialRegistry {

    private class Entry(
        val material: Material,
        val version: Int,
    )

    private val materials = Registry<Entry>()
    private var nextVersion = 0

    val size: Int
        get() = materials.size

    fun add(material: Material): MaterialId = MaterialId(materials.add(Entry(material.deepCopy(), nextVersion++)))

    /**
     * Returns a copy of the material registered under [materialId].
     *
     * @throws IllegalArgumentException if the material does not exist
     */
    operator fun get(materialId: MaterialId): Material = resolve(materialId).deepCopy()

    /**
     * Returns the stored snapshot without copying. Intended for the render loop, which must not mutate it.
     */
    fun resolve(materialId: MaterialId): Material =
        materials[materialId.value]?.material ?: throw IllegalArgumentException("Unknown material $materialId")

    fun resolveOrNull(materialId: MaterialId): Material? = materials[materialId.value]?.material

    /**
     * A number that changes whenever the material stored under [materialId] is replaced or removed,
     * so renderers can cache derived state such as pipelines. Versions are unique across all materials,
     * so a recycled id never reuses an old version. Returns -1 for unknown ids.
     */
    fun versionOf(materialId: MaterialId): Int = materials[materialId.value]?.version ?: -1

    operator fun contains(materialId: MaterialId): Boolean = materialId.value in materials

    fun update(materialId: MaterialId, material: Material) {
        require(materialId in this) { "Unknown material $materialId" }
        materials[materialId.value] = Entry(material.deepCopy(), nextVersion++)
    }

    fun remove(materialId: MaterialId) {
        requireNotNull(materials.remove(materialId.value)) { "Unknown material $materialId" }
    }
}
