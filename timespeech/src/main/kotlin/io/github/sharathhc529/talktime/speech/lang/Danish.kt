package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Danish : TimePhraseGenerator {
    private val locale = Locale("da")

    private val units = listOf("nul", "et", "to", "tre", "fire", "fem", "seks", "syv", "otte", "ni")
    private val teens = listOf(
        "ti", "elleve", "tolv", "tretten", "fjorten", "femten", "seksten", "sytten", "atten", "nitten",
    )
    private val tens = listOf("", "", "tyve", "tredive", "fyrre", "halvtreds")

    /** Danish numbers 0-59 ("énogtyve", "toogtredive", ...). */
    fun number(n: Int): String = when {
        n < 10 -> units[n]
        n < 20 -> teens[n - 10]
        n % 10 == 0 -> tens[n / 10]
        else -> (if (n % 10 == 1) "én" else units[n % 10]) + "og" + tens[n / 10]
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        var text = if (options.style == TimeStyle.COMMON) common(time, options.timeOfDay)
        else formal(time, options.is24Hour, options.seconds, options.timeOfDay)
        if (options.introText) text = "Den er $text"
        return text.capitalizeFirst(locale)
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        return if (time.isPm) when (hour) {
            in 1..5 -> "om eftermiddagen"
            in 6..11 -> "om aftenen"
            else -> "om natten"
        } else when (hour) {
            in 0..4 -> "om natten"
            in 5..8 -> "om morgenen"
            else -> "om formiddagen"
        }
    }

    private fun minutes(n: Int) = number(n) + if (n == 1) " minut" else " minutter"

    private fun common(time: ClockTime, timeOfDay: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = number(if (hour == 12) 1 else hour + 1)
        val text = when {
            minute == 15 -> "kvart over ${number(hour)}"
            minute == 45 -> "kvart i $next"
            minute == 30 -> "halv $next"
            minute in 1..20 -> minutes(minute) + " over " + number(hour)
            minute in 21..29 -> minutes(30 - minute) + " i halv " + next
            minute in 31..39 -> minutes(minute - 30) + " over halv " + next
            minute in 40..59 -> minutes(60 - minute) + " i " + next
            hour == 12 -> if (time.isPm) "middag" else "midnat"
            else -> number(hour)
        }
        return if (timeOfDay) text + " " + dayPart(time) else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, timeOfDay: Boolean): String {
        val hour = number(if (is24Hour) time.hour else time.dialHour)
        val minute = time.minute
        val text = when {
            withSeconds -> "$hour ${minutes(minute)} og ${number(time.second)} " +
                if (time.second == 1) "sekund" else "sekunder"
            minute == 0 -> hour
            minute in 1..9 -> "$hour ${number(0)} ${number(minute)}"
            else -> "$hour ${number(minute)}"
        }
        return if (!timeOfDay || is24Hour) text else text + " " + dayPart(time)
    }
}
