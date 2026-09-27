package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Dutch : TimePhraseGenerator {
    private val locale = Locale("nl")

    private val units = listOf("", "een", "twee", "drie", "vier", "vijf", "zes", "zeven", "acht", "negen")
    private val teens = listOf(
        "tien", "elf", "twaalf", "dertien", "veertien", "vijftien", "zestien", "zeventien", "achttien", "negentien",
    )
    private val tens = listOf("", "", "twintig", "dertig", "veertig", "vijftig")

    /** Dutch number words for 0-59 ("eenentwintig", "tweeëntwintig", ...). */
    fun number(n: Int): String = when {
        n == 0 -> "nul"
        n < 10 -> units[n]
        n < 20 -> teens[n - 10]
        n % 10 == 0 -> tens[n / 10]
        else -> {
            val unit = units[n % 10]
            // "twee" and "drie" take a diaeresis before "en": tweeëntwintig, drieëndertig.
            val link = if (unit.endsWith("e")) "ën" else "en"
            unit + link + tens[n / 10]
        }
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time)
            if (options.timeOfDay && !base.startsWith("middernacht") && !base.startsWith("middag")) base + dayPart(time) else base
        } else {
            val base = formal(time, options.is24Hour, options.seconds)
            if (options.timeOfDay && !options.is24Hour) base + dayPart(time) else base
        }
        return if (options.introText) "Het is $text" else text.capitalizeFirst(locale)
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        return when {
            time.isPm -> if (hour in 6..11) " 's avonds" else " 's namiddags"
            hour < 6 -> " 's nachts"
            else -> " 's morgens"
        }
    }

    private fun minuteWord(n: Int) = if (n == 1) "minuut" else "minuten"

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        var text = number(hour) + " uur" + if (minute == 0) "" else " en ${number(minute)} ${minuteWord(minute)}"
        if (!withSeconds) return text
        if (minute == 0) text += " en ${number(0)} ${minuteWord(0)}"
        val second = time.second
        return text + " en ${number(second)} " + if (second == 1) "seconde" else "seconden"
    }

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = number((if (hour == 12) 0 else hour) + 1)
        return when {
            minute == 15 -> "kwart over ${number(hour)}"
            minute == 45 -> "kwart voor $next"
            minute == 30 -> "half $next"
            minute == 0 && hour == 12 -> if (time.isPm) "middag" else "middernacht"
            minute == 0 -> number(hour) + " uur"
            minute in 1..14 -> "${number(minute)} over ${number(hour)}"
            minute in 16..29 -> "${number(30 - minute)} voor half $next"
            minute in 31..44 -> "${number(minute - 30)} over half $next"
            else -> "${number(60 - minute)} voor $next"
        }
    }
}
