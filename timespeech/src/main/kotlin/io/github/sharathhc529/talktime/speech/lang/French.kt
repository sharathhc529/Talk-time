package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object French : TimePhraseGenerator {
    private val locale = Locale.FRENCH

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time)
            if (!options.timeOfDay || base.endsWith("minuit") || base.endsWith("midi")) base
            else base + dayPart(time.isPm, time.hourOfHalfDay, 7)
        } else {
            val hour = if (options.is24Hour) time.hour else time.dialHour
            val base = formal(time, hour, options.seconds)
            if (options.is24Hour || !options.timeOfDay) base else base + dayPart(time.isPm, hour, 6)
        }
        return if (options.introText) "Il est $text" else text.capitalizeFirst(locale)
    }

    private fun dayPart(isPm: Boolean, hour: Int, eveningFrom: Int) = when {
        !isPm -> " du matin"
        hour in eveningFrom..11 -> " du soir"
        else -> " de l’après-midi"
    }

    private fun hours(hour: Int) = if (hour == 1) "une heure" else "$hour heures"

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        return when (minute) {
            15 -> hours(hour) + " et quart"
            45 -> hours(next) + " moins le quart"
            30 -> when {
                time.isPm && hour == 12 -> "midi et demi"
                hour == 12 -> "minuit et demi"
                else -> hours(hour) + " et demie"
            }
            in 1..29 -> hours(hour) + " $minute"
            in 31..59 -> hours(next) + " moins ${60 - minute}"
            else -> when {
                time.isPm && hour == 12 -> "midi"
                hour == 12 -> "minuit"
                else -> hours(hour)
            }
        }
    }

    private fun formal(time: ClockTime, hour: Int, withSeconds: Boolean): String {
        val minute = time.minute
        val hourText = if (hour == 1) "une heure" else (if (hour == 21) "vingt et une" else "$hour") + " heures"
        var text = if (minute == 0) hourText else "$hourText $minute"
        if (!withSeconds) return text
        if (minute == 0) text += " et 0 minute"
        return text + if (time.second == 1) " et un seconde" else " et ${time.second} secondes"
    }
}
