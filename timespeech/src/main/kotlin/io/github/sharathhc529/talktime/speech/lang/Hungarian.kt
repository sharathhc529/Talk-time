package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Hungarian : TimePhraseGenerator {
    private val locale = Locale("hu")

    override val supportsSeconds = false

    private val units = listOf("", "egy", "kettő", "három", "négy", "öt", "hat", "hét", "nyolc", "kilenc")
    private val tens = listOf("", "tizen", "huszon", "harminc", "negyven", "ötven")
    private val roundTens = listOf("nulla", "tíz", "húsz", "harminc", "negyven", "ötven")

    fun number(n: Int): String = when {
        // "két" is the attributive form used before a noun ("tizenkét óra").
        n == 12 || n == 52 -> tens[n / 10] + "két"
        n % 10 == 0 -> roundTens[n / 10]
        n < 10 -> units[n]
        else -> tens[n / 10] + units[n % 10]
    }

    private const val INTRO = "A pontos idő"

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) common(time, options.introText, options.timeOfDay)
        else formal(time, options.is24Hour, options.introText, options.timeOfDay)
        return text.capitalizeFirst(locale)
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        return if (time.isPm) when (hour) {
            in 1..5 -> "délután"
            in 6..9 -> "este"
            else -> "éjjel"
        } else when (hour) {
            in 0..3 -> "éjjel"
            in 4..7 -> "hajnali"
            in 8..9 -> "reggel"
            else -> "délelőtt"
        }
    }

    private fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        var hour = time.hourOfHalfDay
        if (hour == 0 && time.isPm) hour = 12
        val minute = time.minute
        val next = number(if (hour == 12) 1 else hour + 1)
        var withDayPart = timeOfDay
        var text = when (minute) {
            0 -> if (hour == 12 || hour == 0) {
                withDayPart = false
                if (time.isPm) "dél" else "éjfél"
            } else "${number(hour)} óra"
            15 -> "negyed $next"
            30 -> "fél $next"
            45 -> "háromnegyed $next"
            else -> "${number(hour)} óra ${number(minute)} perc"
        }
        if (withDayPart) text = dayPart(time) + " " + text
        return if (intro) "$INTRO $text" else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, intro: Boolean, timeOfDay: Boolean): String {
        val hour = when {
            is24Hour -> time.hour
            time.hourOfHalfDay == 0 && time.isPm -> 12
            else -> time.hourOfHalfDay
        }
        val hourText = number(hour)
        var text = when (time.minute) {
            0 -> "$hourText óra"
            15 -> "negyed $hourText"
            30 -> "fél $hourText"
            45 -> "háromnegyed $hourText"
            else -> "$hourText óra ${number(time.minute)} perc"
        }
        if (timeOfDay && !is24Hour) text = dayPart(time) + " " + text
        return if (intro) "$INTRO $text" else text
    }
}
