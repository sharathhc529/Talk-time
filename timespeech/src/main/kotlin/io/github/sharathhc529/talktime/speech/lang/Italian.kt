package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Italian : TimePhraseGenerator {
    private val locale = Locale.ITALIAN

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        if (options.style == TimeStyle.COMMON) {
            val base = common(time, options.introText)
            val isNoonOrMidnight = listOf("È mezzanotte", "È mezzogiorno", "Mezzanotte", "Mezzogiorno").any { base.startsWith(it) }
            return if (!options.timeOfDay || isNoonOrMidnight) base else base + dayPart(time.isPm, time.hourOfHalfDay)
        }
        if (options.is24Hour) return formal(time, time.hour, options.introText, options.seconds)
        val hour = time.dialHour
        val base = formal(time, hour, options.introText, options.seconds)
        return if (options.timeOfDay) base + dayPart(time.isPm, hour) else base
    }

    private fun dayPart(isPm: Boolean, hour: Int) = when {
        !isPm -> " di mattina"
        hour in 7..9 -> " di sera"
        hour in 10..11 -> " di notte"
        else -> " di pomeriggio"
    }

    private fun formal(time: ClockTime, hour: Int, intro: Boolean, withSeconds: Boolean): String {
        val plural = if (intro) "Sono le " else ""
        val singular = if (intro) "È " else ""
        val minute = time.minute
        var text = when {
            hour == 1 -> singular + "l'una" + if (minute == 0) "" else " e $minute"
            minute == 0 -> plural + if (hour == 0) "24" else "$hour"
            else -> plural + "$hour e $minute"
        }
        if (withSeconds) {
            if (minute == 0) text += " e 0 minuti"
            text += if (time.second == 1) " e un secondo" else " e ${time.second} secondi"
        }
        return if (intro) text else text.capitalizeFirst(locale)
    }

    private fun common(time: ClockTime, intro: Boolean): String {
        val plural = if (intro) "Sono le " else ""
        val singular = if (intro) "È " else ""
        var hour = time.dialHour
        val minute = time.minute
        val pm = time.isPm
        // Names the hour after "È"/"Sono le", using noon/midnight for 12 o'clock.
        fun named(h: Int, noonPm: Boolean): String? = when {
            h == 1 -> singular + "l'una"
            h == 12 && noonPm -> singular + "mezzogiorno"
            h == 12 -> singular + "mezzanotte"
            else -> null
        }
        val text = when {
            minute == 15 -> named(hour, pm)?.plus(" e un quarto") ?: "$plural$hour e un quarto"
            minute == 45 -> {
                val next = (if (hour == 12) 0 else hour) + 1
                when {
                    !pm && hour == 11 -> singular + "mezzogiorno meno un quarto"
                    pm && hour == 11 -> singular + "mezzanotte meno un quarto"
                    next == 1 -> singular + "l'una meno un quarto"
                    else -> "$plural$next meno un quarto"
                }
            }
            minute == 30 -> named(hour, pm)?.plus(" e mezza") ?: "$plural$hour e mezza"
            minute == 0 && hour == 12 -> singular + if (pm) "mezzogiorno" else "mezzanotte"
            minute == 0 -> if (hour == 1) singular + "l'una" else "$plural$hour"
            minute == 50 || minute == 55 -> {
                if (hour == 12) hour = 0
                val remaining = 60 - minute
                val next = hour + 1
                when {
                    !pm && hour == 11 -> singular + "mezzogiorno meno $remaining"
                    pm && hour == 11 -> singular + "mezzanotte meno $remaining"
                    next == 1 -> singular + "l'una meno $remaining"
                    else -> "$plural$next meno $remaining"
                }
            }
            else -> named(hour, pm)?.plus(" e $minute") ?: "$plural$hour e $minute"
        }
        return if (intro) text else text.capitalizeFirst(locale)
    }
}
