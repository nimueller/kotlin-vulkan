package dev.cryptospace.anvil.core.collections

/**
 * A high-performance registry implementation optimized for frequent access and modifications.
 * Particularly suitable for performance-critical scenarios such as managing mesh and material
 * references during render operations and draw calls.
 *
 * The implementation uses an array-based storage with efficient slot recycling to minimize
 * memory allocations and provide O(1) access times.
 *
 * Note that indices of removed objects are recycled, so a stale index may later refer to a different object.
 *
 * @param T The type of objects stored in the registry
 * @param initial Initial capacity of the registry (default: 64)
 */
class Registry<T>(
    initial: Int = 64,
) {
    private var items = arrayOfNulls<Slot<T>>(maxOf(1, initial))
    private val free = ArrayDeque<Int>()
    private var nextUnusedIndex = 0

    /**
     * Number of objects currently stored in the registry.
     */
    var size: Int = 0
        private set

    private class Slot<T>(
        val slotValue: T,
    )

    /**
     * Adds an object to the registry with O(1) amortized time complexity.
     * Reuses previously freed slots when available to minimize memory fragmentation
     * and allocation overhead.
     *
     * @param obj The object to add to the registry
     * @return The index assigned to the added object, guaranteed to be valid until the object is removed
     */
    fun add(obj: T): Int {
        val index = free.removeFirstOrNull() ?: takeUnusedIndex()
        items[index] = Slot(obj)
        size++
        return index
    }

    /**
     * Replaces the object stored at [index].
     *
     * @throws IllegalArgumentException if no object is stored at [index]
     */
    operator fun set(index: Int, obj: T) {
        require(contains(index)) { "No object registered at index $index" }
        items[index] = Slot(obj)
    }

    fun getAll(): List<T> = items.mapNotNull { it?.slotValue }

    private fun takeUnusedIndex(): Int {
        if (nextUnusedIndex == items.size) {
            items = items.copyOf(items.size * 2)
        }
        return nextUnusedIndex++
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
     * Checks whether an object is currently stored at [index].
     */
    operator fun contains(index: Int): Boolean = items.getOrNull(index) != null

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
        size--
        return previousItem.slotValue
    }
}
