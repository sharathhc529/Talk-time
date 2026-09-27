package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

/** Portuguese (Portugal). */
object Portuguese : PortugueseBase(brazilian = false)

/** Portuguese (Brazil). */
object BrazilianPortuguese : PortugueseBase(brazilian = true)

abstract class PortugueseBase(private val brazilian: Boolean) : TimePhraseGenerator {

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        if (options.style == TimeStyle.COMMON) {
            val base = common(time, options.introText)
            val isNoonOrMidnight = listOf("meia-noite", "meio-dia", "Meia-noite", "Meio-dia").any { base.endsWith(it) }
            return if (!options.timeOfDay || isNoonOrMidnight) base else base + dayPart(time)
        }
        if (options.is24Hour) return formal(time, time.hour, options.introText, options.seconds)
        val base = formal(time, time.dialHour, options.introText, options.seconds)
        return if (options.timeOfDay) base + dayPart(time) else base
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        val minute = time.minute
        return when {
            time.isPm -> if (hour in 7..11 || (hour == 6 && minute > 30)) " da noite" else " da tarde"
            hour == 0 && minute <= 30 -> " da noite"
            hour in 0..4 || (hour == 5 && minute <= 30) -> " da madrugada"
            hour in 5..10 || (hour == 11 && minute <= 30) -> " da manhã"
            else -> " da tarde"
        }
    }

    /** "É uma" / "São duas" / "São 5", optionally without the verb. */
    private fun hourPhrase(hour: Int, intro: Boolean, suffix: String): String = when (hour) {
        1 -> (if (intro) "É uma" else "Uma") + suffix
        2 -> (if (intro) "São " else "") + "duas" + suffix
        else -> (if (intro) "São " else "") + hour + suffix
    }

    private fun formal(time: ClockTime, hour: Int, intro: Boolean, withSeconds: Boolean): String {
        val minute = time.minute
        var text = hourPhrase(hour, intro, " hora" + if (hour == 1) "" else "s")
        if (minute == 0 && brazilian) return text
        when (minute) {
            0 -> {}
            1 -> text += " e um minuto"
            else -> text += " e $minute minutos"
        }
        if (!withSeconds) return text
        if (minute == 0) text += " e 0 minutos"
        return text + if (time.second == 1) " e um segundo" else " e ${time.second} segundo"
    }

    private fun common(time: ClockTime, intro: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        val plural = if (intro) "São " else ""
        val missing = if (intro) "Faltam " else ""
        return when {
            minute == 15 && brazilian -> if (hour == 1) hourPhrase(1, intro, " e quinze") else "$plural$hour e quinze"
            minute == 15 -> if (hour == 1) hourPhrase(1, intro, " e um quarto") else "$plural$hour e um quarto"
            minute == 45 && brazilian -> missing + "quinze " + toHour(next)
            minute == 45 -> if (next == 1) hourPhrase(1, intro, " menos um quarto") else hourPhrase(next, intro, " menos um quarto")
            minute == 30 -> if (hour == 2) plural + "duas horas e meia" else hourPhrase(hour, intro, " e meia")
            minute == 0 && hour == 12 && time.isPm -> if (intro) "É meio-dia" else "Meio-dia"
            minute == 0 && hour == 12 -> if (intro) "É meia-noite" else "Meia-noite"
            minute == 0 -> hourPhrase(hour, intro, " hora" + if (hour == 1) "" else "s")
            minute in 1..29 -> hourPhrase(hour, intro, if (minute == 1) " e um" else " e $minute")
            brazilian -> missing + "${60 - minute} " + toHour(next)
            else -> {
                val remaining = 60 - minute
                val start = if (next == 1) hourPhrase(1, intro, "") else "$plural$next"
                start + if (remaining == 1) " menos um minuto" else " menos $remaining minutos"
            }
        }
    }

    /** Brazilian "pra uma" / "pras duas" / "pras 5". */
    private fun toHour(hour: Int) = when (hour) {
        1 -> "pra uma"
        2 -> "pras duas"
        else -> "pras $hour"
    }
}
