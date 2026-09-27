package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Indonesian : TimePhraseGenerator {
    private val locale = Locale("id")

    private val units = listOf("nol", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan", "sembilan")

    fun number(n: Int): String = when {
        n < 10 -> units[n]
        n == 10 -> "sepuluh"
        n == 11 -> "sebelas"
        n < 20 -> units[n % 10] + " belas"
        n % 10 == 0 -> units[n / 10] + " puluh"
        else -> units[n / 10] + " puluh " + units[n % 10]
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) common(time, options.introText, options.timeOfDay)
        else formal(time, options.is24Hour, options.seconds, options.introText, options.timeOfDay)
        return if (text.isEmpty()) text else text.capitalizeFirst(locale)
    }

    private fun withDayPart(time: ClockTime, text: String): String {
        if (text.contains("tengah hari") || text.contains("tengah malam")) return text
        val hour = time.dialHour
        val pm = time.isPm
        return when {
            (!pm && (hour in 1..5 || hour == 12)) || (pm && hour in 9..11) -> "$text malam"
            !pm && hour in 6..10 -> "$text pagi"
            !pm && hour == 11 -> "$text siang"
            pm && hour in 5..8 -> "$text sore"
            else -> text
        }
    }

    private fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = number((if (hour == 12) 0 else hour) + 1)
        var text = when {
            minute == 15 -> "jam ${number(hour)} semperempat"
            minute == 45 -> "jam $next kurang semperempat"
            minute == 30 -> "jam setengah $next"
            minute in 1..29 -> "jam ${number(hour)} lewat ${number(minute)}"
            minute in 31..59 -> "jam $next kurang ${number(60 - minute)}"
            hour == 12 -> if (time.isPm) "tengah hari" else "tengah malam"
            else -> "jam ${number(hour)}"
        }
        if (timeOfDay) text = withDayPart(time, text)
        return if (intro) "Sekarang $text" else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, intro: Boolean, timeOfDay: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        var text = "jam " + number(hour)
        if (minute > 0) text += " " + number(minute)
        if (withSeconds && time.second != 0) {
            if (minute > 0) text += " minit"
            text += " dan ${number(time.second)} detik"
        }
        if (timeOfDay && !is24Hour) text = withDayPart(time, text)
        return if (intro) "Sekarang $text" else text
    }
}
