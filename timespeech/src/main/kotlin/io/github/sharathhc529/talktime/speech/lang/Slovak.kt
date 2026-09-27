package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Slovak : TimePhraseGenerator {
    private val locale = Locale("sk")

    private val ordinals = listOf(
        "", "jednej", "druhej", "tretej", "štvrtej", "piatej", "šiestej",
        "siedmej", "ôsmej", "deviatej", "desiatej", "jedenástej", "dvanástej",
    )

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val common = options.style == TimeStyle.COMMON
        val base = if (common) common(time, options.introText)
        else formal(time, options.is24Hour, options.introText, options.seconds)
        val isNoonOrMidnight = listOf("polnoc", "poludnie", "Polnoc", "Poludnie").any { base.endsWith(it) }
        if (!options.timeOfDay || !(common || !options.is24Hour) || isNoonOrMidnight) return base
        val hour = time.dialHour
        return base + when {
            !time.isPm -> " ráno"
            hour in 7..11 -> " večer"
            else -> " poobede"
        }
    }

    private fun number(n: Int, accusative: Boolean) = when (n) {
        1 -> if (accusative) "jednu" else "jedna"
        2 -> "dve"
        else -> n.toString()
    }

    private fun minutes(count: Int, nominative: Boolean) = when (count) {
        1 -> if (nominative) "minúta" else "minútu"
        in 2..4 -> "minúty"
        else -> "minút"
    }

    private fun hours(count: Int) = when (count) {
        1 -> "hodina"
        in 2..4 -> "hodiny"
        else -> "hodín"
    }

    private fun seconds(count: Int) = when (count) {
        1 -> "sekunda"
        in 2..4 -> "sekundy"
        else -> "sekúnd"
    }

    private fun common(time: ClockTime, intro: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = if (hour == 12) 1 else hour + 1
        var verb = if (!intro) "" else if (minute % 15 == 0) "Je " else "O "
        fun inMinutes(count: Int) = number(count, true) + " " + minutes(count, false)
        val text = when (minute) {
            0 -> when {
                hour != 12 -> {
                    if (intro && hour in 2..4) verb = "Sú "
                    number(hour, false) + " " + hours(hour)
                }
                time.isPm -> "poludnie"
                else -> "polnoc"
            }
            15 -> "štvrť na " + number(next, true)
            30 -> "pol " + ordinals[next]
            45 -> "tri štvrte na " + number(next, true)
            in 1..14 -> inMinutes(15 - minute) + " bude štvrť na " + number(next, true)
            in 16..29 -> inMinutes(30 - minute) + " bude pol " + ordinals[next]
            in 31..44 -> inMinutes(45 - minute) + " bude tri štvrte na " + number(next, true)
            else -> inMinutes(60 - minute) + " " + (if (next == 2) "budu" else "bude") + " " +
                number(next, false) + " " + hours(next)
        }
        val sentence = verb + text
        return if (intro) sentence else sentence.capitalizeFirst(locale)
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, intro: Boolean, withSeconds: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        val verb = if (!intro) "" else if (hour in 2..4) "Sú " else "Je "
        val rest = when {
            withSeconds -> " ${number(minute, false)} ${minutes(minute, true)} a " +
                "${number(time.second, false)} ${seconds(time.second)}"
            minute > 0 -> " a ${number(minute, false)} ${minutes(minute, true)}"
            else -> ""
        }
        return "$verb${number(hour, false)} ${hours(hour)}$rest"
    }
}
