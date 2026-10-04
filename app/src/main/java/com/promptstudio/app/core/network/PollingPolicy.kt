package com.promptstudio.app.core.network

import kotlin.math.min
import kotlin.random.Random
import javax.inject.Inject

/** Produces the documented 2-second to 10-second polling cadence with bounded jitter. */
class PollingPolicy internal constructor(
    private val random: Random,
) {
    @Inject
    constructor() : this(Random.Default)

    fun nextDelayMillis(previousDelayMillis: Long?): Long {
        val base = previousDelayMillis?.let { min((it * GROWTH).toLong(), MAX_DELAY_MILLIS) }
            ?: INITIAL_DELAY_MILLIS
        return base + random.nextLong(0, JITTER_MILLIS + 1)
    }

    companion object {
        const val INITIAL_DELAY_MILLIS = 2_000L
        const val MAX_DELAY_MILLIS = 10_000L
        private const val GROWTH = 1.5
        private const val JITTER_MILLIS = 500L
    }
}
