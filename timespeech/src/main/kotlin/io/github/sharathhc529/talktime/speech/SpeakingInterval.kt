package io.github.sharathhc529.talktime.speech

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** How often the interval speaking clock announces the time. */
enum class SpeakingInterval(val usesSeconds: Boolean = false) {
    EVERY_15_SECONDS(usesSeconds = true),
    EVERY_20_SECONDS(usesSeconds = true),
    EVERY_30_SECONDS(usesSeconds = true),
    EVERY_MINUTE,
    EVERY_EVEN_MINUTE,
    EVERY_ODD_MINUTE,
    EVERY_5_MINUTES,
    EVERY_10_MINUTES,
    EVERY_15_MINUTES,
    EVERY_20_MINUTES,
    EVERY_30_MINUTES,
    EVERY_HOUR,
    ;

    /** The first announcement time strictly after [now], aligned to the interval (e.g. xx:05, xx:10). */
    fun nextAfter(now: LocalDateTime): LocalDateTime {
        val minute = now.minute
        val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        return when (this) {
            EVERY_15_SECONDS -> secondsStep(now, 15)
            EVERY_20_SECONDS -> secondsStep(now, 20)
            EVERY_30_SECONDS -> secondsStep(now, 30)
            EVERY_MINUTE -> nextMinute
            EVERY_EVEN_MINUTE -> if (minute % 2 != 0) nextMinute else nextMinute.plusMinutes(1)
            EVERY_ODD_MINUTE -> if (minute % 2 == 0) nextMinute else nextMinute.plusMinutes(1)
            EVERY_5_MINUTES -> nextMinute.plusMinutes((4 - minute % 5).toLong())
            EVERY_10_MINUTES -> nextMinute.plusMinutes((9 - minute % 10).toLong())
            EVERY_15_MINUTES -> nextMinute.plusMinutes((14 - minute % 15).toLong())
            EVERY_20_MINUTES -> nextMinute.plusMinutes((19 - minute % 20).toLong())
            EVERY_30_MINUTES -> nextMinute.plusMinutes((29 - minute % 30).toLong())
            EVERY_HOUR -> nextMinute.plusMinutes((59 - minute).toLong())
        }
    }

    private fun secondsStep(now: LocalDateTime, step: Int): LocalDateTime {
        val base = now.truncatedTo(ChronoUnit.SECONDS)
        return base.plusSeconds((step - now.second % step).toLong())
    }
}
