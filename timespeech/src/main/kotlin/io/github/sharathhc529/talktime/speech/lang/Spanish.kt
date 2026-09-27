package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

/** Spanish (Spain). */
object Spanish : SpanishBase() {
    override fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val base = commonSpain(time, intro)
        val isNoonOrMidnight = listOf("medianoche", "mediodía", "Medianoche", "Mediodía").any { base.endsWith(it) }
        return if (!timeOfDay || isNoonOrMidnight) base else base + dayPart(time)
    }

    private fun commonSpain(time: ClockTime, intro: Boolean): String {
        val plural = if (intro) "Son las " else ""
        val singular = if (intro) "Es la " else ""
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        fun at(h: Int, rest: String) = if (h == 1) singular + "una" + rest else "$plural$h$rest"
        val text = when {
            minute == 15 -> at(hour, " y cuarto")
            minute == 45 -> at(next, " menos cuarto")
            minute == 30 -> at(hour, " y media")
            minute == 0 && hour == 12 -> (if (intro) "Es " else "") + if (time.isPm) "mediodía" else "medianoche"
            minute == 0 -> at(hour, "")
            minute in 1..29 -> at(hour, " y $minute")
            else -> at(next, " menos ${60 - minute}")
        }
        return if (intro) text else text.capitalizeFirst(locale)
    }
}

/** Spanish as spoken in the Americas (United States, Mexico). */
object LatinAmericanSpanish : SpanishBase() {
    override fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val plural = if (intro) "Son las " else ""
        val singular = if (intro) "Es la " else ""
        val missingMany = if (intro) "Faltan " else ""
        val missingOne = if (intro) "Falta " else ""
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        val part = dayPart(time)
        fun at(h: Int, rest: String) = if (h == 1) singular + "una" + rest else "$plural$h$rest"
        fun forHour(h: Int) = if (h == 1) "para la una" else "para las $h"
        val text = when {
            minute == 15 -> if (timeOfDay) at(hour, "$part con 15 minutos") else at(hour, " y cuarto")
            minute == 45 -> if (timeOfDay) "${missingMany}15 minutos ${forHour(next)}$part" else missingOne + "un cuarto " + forHour(next)
            minute == 30 -> if (timeOfDay) at(hour, "$part con 30 minutos") else at(hour, " y media")
            minute == 0 && hour == 12 -> (if (intro) "Es " else "") + if (time.isPm) "mediodía" else "medianoche"
            minute == 0 -> at(hour, if (timeOfDay) part else "")
            minute in 1..29 -> when {
                !timeOfDay -> at(hour, " y $minute")
                minute == 1 -> at(hour, "$part con un minuto")
                else -> at(hour, "$part con $minute minutos")
            }
            else -> {
                val remaining = 60 - minute
                val missing = if (remaining == 1) missingOne else missingMany
                when {
                    !timeOfDay -> "$missing$remaining ${forHour(next)}"
                    remaining == 1 -> missingOne + "un minuto " + forHour(next) + part
                    else -> "$missingMany$remaining minutos ${forHour(next)}$part"
                }
            }
        }
        return if (intro) text else text.capitalizeFirst(locale)
    }
}

abstract class SpanishBase : TimePhraseGenerator {
    protected val locale = Locale("es")

    protected abstract fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        if (options.style == TimeStyle.COMMON) return common(time, options.introText, options.timeOfDay)
        val base = formal(time, options.is24Hour, options.introText, options.seconds)
        return if (options.timeOfDay) base + dayPart(time) else base
    }

    protected fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        val minute = time.minute
        return when {
            time.isPm -> if (hour in 7..11 || (hour == 6 && minute > 30)) " de la noche" else " de la tarde"
            hour == 0 && minute <= 30 -> " de la noche"
            hour in 0..4 || (hour == 5 && minute <= 30) -> " de la madrugada"
            hour in 5..10 || (hour == 11 && minute <= 30) -> " de la mañana"
            else -> " de la tarde"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, intro: Boolean, withSeconds: Boolean): String {
        val plural = if (intro) "Son las " else ""
        val singular = if (intro) "Es la " else ""
        val minute = time.minute
        var text = if (is24Hour) {
            val hour = time.hour
            (if (hour == 1) singular + "una horas" else "$plural$hour horas") + when (minute) {
                0 -> ""
                1 -> " y un minuto"
                else -> " y $minute minutos"
            }
        } else {
            val hour = time.dialHour
            (if (hour == 1) singular + "una" else "$plural$hour") + if (minute == 0) "" else " $minute"
        }
        if (withSeconds) {
            if (minute == 0) text += " y 0 minutos"
            text += if (time.second == 1) " y un segundo" else " y ${time.second} segundos"
        }
        return if (intro) text else text.capitalizeFirst(locale)
    }
}
