package dj.nomade.velox_restaurant.core.utils

import kotlinx.coroutines.delay

suspend fun <T> withRetry(
    times: Int = 3,
    initialDelayMs: Long = 500L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(times - 1) {
        try {
            return block()
        } catch (_: Exception) { }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }
    return block() // dernière tentative — laisse l'exception se propager
}
