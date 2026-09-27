package io.github.sharathhc529.talktime.speech

import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

/** How the time is phrased: "13:25" style (formal) or "twenty-five past one" style (common). */
enum class TimeStyle { FORMAL, COMMON }

/** A wall-clock time of day, independent of time zones. */
data class ClockTime(val hour: Int, val minute: Int, val second: Int = 0) {
    init {
        require(hour in 0..23 && minute in 0..59 && second in 0..59) { "Invalid time $hour:$minute:$second" }
    }

    /** Hour on a 12-hour dial where midnight/noon are 0 (like [Calendar.HOUR]). */
    val hourOfHalfDay: Int get() = hour % 12

    /** Hour on a 12-hour dial where midnight/noon are 12. */
    val dialHour: Int get() = if (hourOfHalfDay == 0) 12 else hourOfHalfDay

    val isPm: Boolean get() = hour >= 12

    companion object {
        fun of(calendar: Calendar) = ClockTime(
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
        )

        fun of(time: LocalDateTime) = ClockTime(time.hour, time.minute, time.second)

        fun now(): ClockTime = of(LocalDateTime.now())
    }
}

/** Options that control how a time announcement is phrased. */
data class PhraseOptions(
    val is24Hour: Boolean,
    val style: TimeStyle,
    /** Append the part of day ("in the morning", "PM", ...). */
    val timeOfDay: Boolean,
    /** Start with an introduction ("It's ..."). */
    val introText: Boolean,
    /** Include the seconds (only used when [TimePhraseGenerator.supportsSeconds]). */
    val seconds: Boolean,
)

/** Converts a time of day into a sentence in one language. */
interface TimePhraseGenerator {
    val supportsSeconds: Boolean get() = true

    fun phrase(time: ClockTime, options: PhraseOptions): String
}

internal fun String.capitalizeFirst(locale: Locale): String =
    if (isEmpty()) this else substring(0, 1).uppercase(locale) + substring(1)
