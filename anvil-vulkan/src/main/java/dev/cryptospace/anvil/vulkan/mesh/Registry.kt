package dev.cryptospace.anvil.vulkan.mesh

/**
 * A high-performance registry implementation optimized for frequent access and modifications.
 * Particularly suitable for performance-critical scenarios such as managing mesh and material
 * references during render operations and draw calls.
 *
 * The implementation uses an array-based storage with efficient slot recycling to minimize
 * memory allocations and provide O(1) access times.
 *
 * @param T The type of objects stored in the registry
 * @param initial Initial capacity of the registry (default: 64)
 */
class Registry<T>(
    initial: Int = 64,
) {
    private var items = arrayOfNulls<Slot<T>>(initial)
    private val free = ArrayDeque<Int>()

    val size: Int
        get() = items.count { it != null }

    private data class Slot<T>(
        var slotValue: T,
    )

    /**
     * Adds an object to the registry with O(1) time complexity.
     * Reuses previously freed slots when available to minimize memory fragmentation
     * and allocation overhead.
     *
     * @param obj The object to add to the registry
     * @return The index assigned to the added object, guaranteed to be valid for the object's lifetime
     */
    fun add(obj: T): Int {
        val index = free.removeFirstOrNull() ?: findFreeSlotIndexInItemsArray()
        items[index] = Slot(obj)
        return index
    }

    private fun findFreeSlotIndexInItemsArray(): Int {
        val firstFreeIndex = items.indexOfFirst { it == null }

        if (firstFreeIndex >= 0) {
            return firstFreeIndex
        }

        val oldSize = items.size
        items = items.copyOf(maxOf(1, oldSize * 2))
        return oldSize
    }

    /**
     * Retrieves the object at the specified index with O(1) time complexity.
     * Optimized for frequent access during render operations.
     *
     * @param index The index of the object to retrieve
     * @return The object at the specified index, or null if the index is invalid or the slot is empty
     */
    operator fun get(index: Int): T? = items.getOrNull(index)?.slotValue

    /**
     * Removes and returns the object at the specified index with O(1) time complexity.
     * The freed slot is immediately recycled and becomes available for future additions,
     * ensuring optimal memory reuse in dynamic scenarios.
     *
     * @param index The index of the object to remove
     * @return The removed object, or null if the index was invalid or the slot was empty
     */
    fun remove(index: Int): T? {
        val previousItem = items.getOrNull(index) ?: return null
        items[index] = null
        free.add(index)
        return previousItem.slotValue
    }
}
