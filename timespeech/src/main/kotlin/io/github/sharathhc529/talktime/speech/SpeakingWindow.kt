package io.github.sharathhc529.talktime.speech

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A daily schedule for the interval speaking clock: it starts at [start] and stops after the
 * announcement at [end] (e.g. 05:00 to 07:00 announces 05:00 … 07:00). A schedule whose end is
 * before its start runs over midnight (22:00 to 06:00).
 */
data class SpeakingWindow(val start: LocalTime, val end: LocalTime) {

    init {
        require(start != end) { "Start and stop time must differ" }
    }

    operator fun contains(time: LocalTime): Boolean {
        val t = time.withNano(0)
        return if (start < end) t >= start && t <= end else t >= start || t <= end
    }

    operator fun contains(time: LocalDateTime): Boolean = time.toLocalTime() in this

    /** Whether [time] is the last announcement of [interval] before the schedule stops. */
    fun isLastAnnouncement(interval: SpeakingInterval, time: LocalDateTime): Boolean =
        time in this && interval.nextAfter(time) !in this

    /** A moment where the speaking clock is switched on ([starts]) or off by the schedule. */
    data class Change(val at: LocalDateTime, val starts: Boolean)

    /**
     * The next start or stop strictly after [now]. The stop comes a minute after [end], so that
     * the announcement at [end] is still made; the clock usually stops right after that one.
     */
    fun nextChangeAfter(now: LocalDateTime): Change {
        val nextStart = nextOccurrence(start, now)
        val nextStop = nextOccurrence(end.plusMinutes(1), now)
        return if (nextStart <= nextStop) Change(nextStart, starts = true) else Change(nextStop, starts = false)
    }

    private fun nextOccurrence(time: LocalTime, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time)
        return if (today > now) today else today.plusDays(1)
    }

    companion object {
        /** Builds a schedule from minutes after midnight, as stored in the settings; null when they are equal. */
        fun ofMinutes(startMinute: Int, endMinute: Int): SpeakingWindow? {
            val start = LocalTime.ofSecondOfDay(startMinute.mod(MINUTES_PER_DAY) * 60L)
            val end = LocalTime.ofSecondOfDay(endMinute.mod(MINUTES_PER_DAY) * 60L)
            return if (start == end) null else SpeakingWindow(start, end)
        }

        const val MINUTES_PER_DAY = 24 * 60
    }
}
