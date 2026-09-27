package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object German : TimePhraseGenerator {
    private val locale = Locale.GERMAN

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val hour = time.dialHour
            val base = common(time, hour)
            if (!options.timeOfDay) base else when {
                time.isPm -> when {
                    hour in 6..10 -> "$base abends"
                    hour == 11 -> "$base nachts"
                    hour == 12 && time.minute == 0 -> base
                    else -> "$base nachmittags"
                }
                hour == 12 && time.minute == 0 -> base
                hour in 1..4 || hour == 12 -> "$base nachts"
                hour in 10..11 -> "$base vormittags"
                else -> "$base morgens"
            }
        } else {
            val base = if (options.seconds) withSeconds(time, options.is24Hour) else formal(time, options.is24Hour)
            if (options.timeOfDay && !options.is24Hour) base + if (time.isPm) " nachmittags" else " vormittags" else base
        }
        return if (options.introText) "Es ist $text" else text.capitalizeFirst(locale)
    }

    private fun common(time: ClockTime, hour: Int): String {
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        return when (minute) {
            15 -> "viertel nach $hour"
            45 -> "viertel vor $next"
            30 -> "halb $next"
            in 1..20 -> minutes(minute) + " nach $hour"
            in 21..29 -> minutes(30 - minute) + " vor halb $next"
            in 31..39 -> minutes(minute - 30) + " nach halb $next"
            in 40..59 -> minutes(60 - minute) + " vor $next"
            else -> when {
                time.isPm && hour == 12 -> "Mittags"
                hour == 12 -> "Mitternacht"
                else -> formal(time, false)
            }
        }
    }

    private fun minutes(count: Int) = when {
        count % 5 == 0 -> "$count"
        count == 1 -> "eine Minute"
        else -> "$count Minuten"
    }

    private fun hourText(time: ClockTime, is24Hour: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        return if (hour == 1) "ein Uhr" else "$hour Uhr"
    }

    private fun formal(time: ClockTime, is24Hour: Boolean): String {
        val hour = hourText(time, is24Hour)
        return if (time.minute == 0) hour else "$hour ${time.minute}"
    }

    private fun withSeconds(time: ClockTime, is24Hour: Boolean): String {
        val minutes = if (time.minute == 1) "eine Minute" else "${time.minute} Minuten"
        val seconds = if (time.second == 1) "eine Sekunde" else "${time.second} Sekunden"
        return "${hourText(time, is24Hour)}, $minutes, und $seconds"
    }
}
