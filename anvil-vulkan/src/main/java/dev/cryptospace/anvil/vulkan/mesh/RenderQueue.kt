package dev.cryptospace.anvil.vulkan.mesh

import dev.cryptospace.anvil.core.math.MatrixSource
import dev.cryptospace.anvil.core.scene.Material

/**
 * A reusable list of draw calls, sorted to minimize GPU state changes.
 *
 * Each draw is encoded as one `Long` sort key, so sorting is a primitive array sort without allocations or
 * comparators. Payloads live in parallel arrays that only grow, so steady-state frames allocate nothing.
 *
 * Key layout, from most to least significant bit (the sign bit stays 0):
 * ```
 * | 1 transparent | 10 pipeline | 16 material | 16 mesh | 20 item index |
 * ```
 * Opaque draws therefore come before transparent ones, and draws sharing a pipeline, material and mesh are adjacent.
 *
 * @param M The mesh type
 */
class RenderQueue<M : Any>(
    initialCapacity: Int = 1024,
) {

    var size: Int = 0
        private set

    private var keys = LongArray(initialCapacity)
    private var meshes = arrayOfNulls<Any>(initialCapacity)
    private var materials = arrayOfNulls<Material>(initialCapacity)
    private var transforms = arrayOfNulls<MatrixSource>(initialCapacity)

    /**
     * @param pipeline Index of the pipeline, below [MAX_PIPELINES]
     * @param materialSlot Identifies the material for state sorting, below [MAX_MATERIALS]. Draws with the same slot
     *   must use the same [material].
     * @param meshSlot Identifies the mesh for state sorting, below [MAX_MESHES]
     */
    fun add(
        transparent: Boolean,
        pipeline: Int,
        materialSlot: Int,
        meshSlot: Int,
        mesh: M,
        material: Material?,
        transform: MatrixSource,
    ) {
        require(pipeline in 0 until MAX_PIPELINES) { "Pipeline index $pipeline exceeds the render queue's limit" }
        require(materialSlot in 0 until MAX_MATERIALS) {
            "Material slot $materialSlot exceeds the render queue's limit"
        }
        require(meshSlot in 0 until MAX_MESHES) { "Mesh slot $meshSlot exceeds the render queue's limit" }
        require(size < MAX_ITEMS) { "Render queue is full at $MAX_ITEMS draws" }

        if (size == keys.size) {
            grow()
        }

        val transparentBit = if (transparent) 1L else 0L
        keys[size] = (transparentBit shl TRANSPARENT_SHIFT) or
            (pipeline.toLong() shl PIPELINE_SHIFT) or
            (materialSlot.toLong() shl MATERIAL_SHIFT) or
            (meshSlot.toLong() shl MESH_SHIFT) or
            size.toLong()
        meshes[size] = mesh
        materials[size] = material
        transforms[size] = transform
        size++
    }

    /**
     * Orders the draws by their sort keys. Accessors below then refer to sorted positions.
     */
    fun sort() {
        keys.sort(0, size)
    }

    /**
     * Removes all draws, dropping references to payloads so they can be garbage collected.
     */
    fun clear() {
        meshes.fill(null, 0, size)
        materials.fill(null, 0, size)
        transforms.fill(null, 0, size)
        size = 0
    }

    /**
     * Everything that determines GPU state for the draw at [position]: draws with equal state keys can be batched.
     */
    fun stateKey(position: Int): Long = keys[position] ushr ITEM_BITS

    fun isTransparent(position: Int): Boolean = (keys[position] ushr TRANSPARENT_SHIFT) and 1L == 1L

    fun pipeline(position: Int): Int = ((keys[position] ushr PIPELINE_SHIFT) and PIPELINE_MASK).toInt()

    fun materialSlot(position: Int): Int = ((keys[position] ushr MATERIAL_SHIFT) and MATERIAL_MASK).toInt()

    fun meshSlot(position: Int): Int = ((keys[position] ushr MESH_SHIFT) and MESH_MASK).toInt()

    @Suppress("UNCHECKED_CAST")
    fun mesh(position: Int): M = meshes[itemIndex(position)] as M

    fun material(position: Int): Material? = materials[itemIndex(position)]

    fun transform(position: Int): MatrixSource = transforms[itemIndex(position)]!!

    private fun itemIndex(position: Int): Int = (keys[position] and ITEM_MASK).toInt()

    private fun grow() {
        val capacity = keys.size * 2
        keys = keys.copyOf(capacity)
        meshes = meshes.copyOf(capacity)
        materials = materials.copyOf(capacity)
        transforms = transforms.copyOf(capacity)
    }

    companion object {
        private const val ITEM_BITS = 20
        private const val MESH_BITS = 16
        private const val MATERIAL_BITS = 16
        private const val PIPELINE_BITS = 10

        private const val MESH_SHIFT = ITEM_BITS
        private const val MATERIAL_SHIFT = MESH_SHIFT + MESH_BITS
        private const val PIPELINE_SHIFT = MATERIAL_SHIFT + MATERIAL_BITS
        private const val TRANSPARENT_SHIFT = PIPELINE_SHIFT + PIPELINE_BITS

        private const val ITEM_MASK = (1L shl ITEM_BITS) - 1
        private const val MESH_MASK = (1L shl MESH_BITS) - 1
        private const val MATERIAL_MASK = (1L shl MATERIAL_BITS) - 1
        private const val PIPELINE_MASK = (1L shl PIPELINE_BITS) - 1

        const val MAX_ITEMS = 1 shl ITEM_BITS
        const val MAX_MESHES = 1 shl MESH_BITS
        const val MAX_MATERIALS = 1 shl MATERIAL_BITS
        const val MAX_PIPELINES = 1 shl PIPELINE_BITS
    }
}
