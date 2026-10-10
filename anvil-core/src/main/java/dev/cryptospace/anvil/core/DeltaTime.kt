package dev.cryptospace.anvil.core

private const val MILLISECONDS_PER_SECOND: Long = 1000
private const val NANOSECONDS_PER_SECOND: Long = 1_000_000_000

/**
 * Time elapsed since the previous frame.
 *
 * A regular class rather than a value class on purpose: it is passed to every object's update callback, and value
 * classes are boxed each time they are passed to a lambda, while this is allocated once per frame.
 */
class DeltaTime(
    val seconds: Double,
) {

    val milliseconds: Long
        get() = (seconds * MILLISECONDS_PER_SECOND).toLong()

    val nanoseconds: Long
        get() = (seconds * NANOSECONDS_PER_SECOND).toLong()
}
