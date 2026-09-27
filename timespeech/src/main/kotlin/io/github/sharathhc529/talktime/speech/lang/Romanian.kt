package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Romanian : TimePhraseGenerator {
    private val locale = Locale("ro")

    private enum class Gender { MALE, FEMALE, NEUTER }

    private val units = listOf("zero", "", "", "trei", "patru", "cinci", "șase", "șapte", "opt", "nouă")
    private val teens = listOf(
        "zece", "unsprezece", "", "treisprezece", "paisprezece", "cincisprezece",
        "șaisprezece", "șaptesprezece", "optsprezece", "nouăsprezece",
    )
    private val tens = listOf("", "", "douăzeci", "treizeci", "patruzeci", "cincizeci")

    private fun number(n: Int, gender: Gender): String = when {
        n == 1 -> if (gender == Gender.FEMALE) "un" else "unu"
        n == 2 -> if (gender == Gender.MALE) "doi" else "două"
        n == 12 -> if (gender == Gender.FEMALE) "douăsprezece" else "doisprezece"
        n < 10 -> units[n]
        n < 20 -> teens[n - 10]
        n % 10 == 0 -> tens[n / 10]
        // Compound numbers use the masculine form: "douăzeci și unu", "douăzeci și doi".
        else -> tens[n / 10] + " și " + number(n % 10, Gender.MALE)
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) common(time, options.introText, options.timeOfDay)
        else formal(time, options.is24Hour, options.seconds, options.introText, options.timeOfDay)
        return text.capitalizeFirst(locale)
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        return if (time.isPm) when (hour) {
            in 1..5 -> "după-amiaza"
            in 6..11 -> "seara"
            else -> "noapte"
        } else when {
            hour == 0 && time.minute == 0 -> "noapte"
            hour <= 4 -> "noaptea"
            else -> "dimineața"
        }
    }

    private fun minuteWord(n: Int) = if (n == 1) "minut" else "minute"

    private fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = if (hour == 12) 1 else hour + 1
        // "ora două" is feminine, all other hours are counted in the masculine.
        fun hourName(h: Int) = number(h, if (h == 2) Gender.FEMALE else Gender.MALE)
        val female = Gender.FEMALE
        var text = when {
            minute == 15 -> hourName(hour) + " și un sfert"
            minute == 45 -> hourName(next) + " fără un sfert"
            minute == 30 -> hourName(hour) + " și jumătate"
            minute in 16..29 || minute == 5 || minute == 10 -> hourName(hour) + " și " + number(minute, female)
            minute in 1..14 -> hourName(hour) + " și " + number(minute, female) + " " + minuteWord(minute)
            minute in 31..44 || minute == 50 || minute == 55 -> hourName(next) + " fără " + number(60 - minute, female)
            minute in 46..59 -> {
                val remaining = 60 - minute
                hourName(next) + " fără " + number(remaining, female) + " " + minuteWord(remaining)
            }
            hour == 12 -> if (time.isPm) "amiază" else "miezul nopții"
            else -> hourName(hour)
        }
        if (intro) text = (if (time.isPm && hour == 12 && minute == 0) "E " else "Este ") + text
        return if (timeOfDay) text + " " + dayPart(time) else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, intro: Boolean, timeOfDay: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        val female = Gender.FEMALE
        val hourText = "ora " + number(hour, if (hour == 12 && time.isPm) female else Gender.NEUTER)
        var text = when {
            withSeconds -> "$hourText, ${number(minute, female)} ${minuteWord(minute)}, " +
                "${number(time.second, female)} " + if (time.second == 1) "secundă" else "secunde"
            minute == 0 -> hourText
            minute >= 20 -> "$hourText și ${number(minute, female)} de minute"
            else -> "$hourText și ${number(minute, female)} ${minuteWord(minute)}"
        }
        if (intro) text = "Este $text"
        return if (!timeOfDay || is24Hour) text else text + " " + dayPart(time)
    }
}
