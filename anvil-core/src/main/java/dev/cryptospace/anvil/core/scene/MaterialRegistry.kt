package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.collections.Registry

/**
 * Backend-independent bookkeeping for uploaded materials.
 *
 * Stores defensive snapshots, so callers can keep mutating their [Material] instances without affecting
 * what is rendered until they explicitly call [update].
 */
class MaterialRegistry {

    private val materials = Registry<Material>()

    val size: Int
        get() = materials.size

    fun add(material: Material): MaterialId = MaterialId(materials.add(material.deepCopy()))

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
        materials[materialId.value] ?: throw IllegalArgumentException("Unknown material $materialId")

    fun resolveOrNull(materialId: MaterialId): Material? = materials[materialId.value]

    operator fun contains(materialId: MaterialId): Boolean = materialId.value in materials

    fun update(materialId: MaterialId, material: Material) {
        require(materialId in this) { "Unknown material $materialId" }
        materials[materialId.value] = material.deepCopy()
    }

    fun remove(materialId: MaterialId) {
        requireNotNull(materials.remove(materialId.value)) { "Unknown material $materialId" }
    }
}
