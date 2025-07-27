package dev.cryptospace.anvil.core

private const val MILLISECONDS_PER_SECOND: Long = 1000
private const val NANOSECONDS_PER_SECOND: Long = 1_000_000_000

@JvmInline
value class DeltaTime(
    val seconds: Double,
) {

    val milliseconds: Long
        get() = (seconds * MILLISECONDS_PER_SECOND).toLong()

    val nanoseconds: Long
        get() = (seconds * NANOSECONDS_PER_SECOND).toLong()
}
