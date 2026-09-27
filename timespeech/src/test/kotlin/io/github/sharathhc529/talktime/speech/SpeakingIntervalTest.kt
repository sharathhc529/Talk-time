package io.github.sharathhc529.talktime.speech

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeakingIntervalTest {

    private fun at(hour: Int, minute: Int, second: Int = 0, nano: Int = 0) =
        LocalDateTime.of(2024, 1, 15, hour, minute, second, nano)

    @Test
    fun `minute based intervals align to the clock`() {
        val now = at(10, 7, 42, 500_000_000)
        assertEquals(at(10, 8), SpeakingInterval.EVERY_MINUTE.nextAfter(now))
        assertEquals(at(10, 8), SpeakingInterval.EVERY_EVEN_MINUTE.nextAfter(now))
        assertEquals(at(10, 9), SpeakingInterval.EVERY_ODD_MINUTE.nextAfter(now))
        assertEquals(at(10, 10), SpeakingInterval.EVERY_5_MINUTES.nextAfter(now))
        assertEquals(at(10, 10), SpeakingInterval.EVERY_10_MINUTES.nextAfter(now))
        assertEquals(at(10, 15), SpeakingInterval.EVERY_15_MINUTES.nextAfter(now))
        assertEquals(at(10, 20), SpeakingInterval.EVERY_20_MINUTES.nextAfter(now))
        assertEquals(at(10, 30), SpeakingInterval.EVERY_30_MINUTES.nextAfter(now))
        assertEquals(at(11, 0), SpeakingInterval.EVERY_HOUR.nextAfter(now))
    }

    @Test
    fun `an exact boundary schedules the following one`() {
        assertEquals(at(10, 10), SpeakingInterval.EVERY_5_MINUTES.nextAfter(at(10, 5)))
        assertEquals(at(0, 0).plusDays(1), SpeakingInterval.EVERY_HOUR.nextAfter(at(23, 0)))
        assertEquals(at(10, 5, 30), SpeakingInterval.EVERY_15_SECONDS.nextAfter(at(10, 5, 15)))
    }

    @Test
    fun `second based intervals`() {
        assertEquals(at(10, 5, 20), SpeakingInterval.EVERY_20_SECONDS.nextAfter(at(10, 5, 3)))
        assertEquals(at(10, 6, 0), SpeakingInterval.EVERY_30_SECONDS.nextAfter(at(10, 5, 45, 1)))
    }

    @Test
    fun `next time is always in the future and on the grid`() {
        var now = at(0, 0)
        repeat(5_000) {
            for (interval in SpeakingInterval.entries) {
                val next = interval.nextAfter(now)
                assertTrue(next.isAfter(now), "$interval $now -> $next")
                assertEquals(0, next.nano)
                if (!interval.usesSeconds) assertEquals(0, next.second)
            }
            now = now.plusSeconds(17).plusNanos(123_000_000)
        }
    }
}
