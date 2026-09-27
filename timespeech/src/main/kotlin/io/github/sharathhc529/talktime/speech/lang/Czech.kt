package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Czech : TimePhraseGenerator {
    private val locale = Locale("cs")

    private val ordinals = listOf(
        "", "jedné", "druhé", "třetí", "čtvrté", "páté", "šesté",
        "sedmé", "osmé", "deváté", "desáté", "jedenácté", "dvanácté",
    )

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val common = options.style == TimeStyle.COMMON
        val base = if (common) common(time, options.introText)
        else formal(time, options.is24Hour, options.introText, options.seconds)
        val isNoonOrMidnight = listOf("půlnoc", "poledne", "Půlnoc", "Poledne").any { base.endsWith(it) }
        if (!options.timeOfDay || !(common || !options.is24Hour) || isNoonOrMidnight) return base
        val hour = time.dialHour
        return base + when {
            !time.isPm -> " ráno"
            hour in 7..11 -> " večer"
            else -> " odpoledne"
        }
    }

    private fun minutes(count: Int, nominative: Boolean) = when (count) {
        1 -> if (nominative) "minuta" else "minutu"
        in 2..4 -> "minuty"
        else -> "minut"
    }

    private fun hours(count: Int) = when (count) {
        1 -> "hodina"
        in 2..4 -> "hodiny"
        else -> "hodin"
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
        var verb = if (intro) "Je " else ""
        fun inMinutes(count: Int) = "za $count ${minutes(count, false)}"
        val text = when (minute) {
            0 -> when {
                hour != 12 -> {
                    if (intro && hour in 2..4) verb = "Jsou "
                    "$hour ${hours(hour)}"
                }
                time.isPm -> "poledne"
                else -> "půlnoc"
            }
            15 -> "čtvrt na $next"
            30 -> "půl ${ordinals[next]}"
            45 -> "tři čtvrtě na $next"
            in 1..14 -> inMinutes(15 - minute) + " čtvrt na $next"
            in 16..29 -> inMinutes(30 - minute) + " půl ${ordinals[next]}"
            in 31..44 -> inMinutes(45 - minute) + " tři čtvrtě na $next"
            else -> inMinutes(60 - minute) + " $next ${hours(next)}"
        }
        val sentence = verb + text
        return if (intro) sentence else sentence.capitalizeFirst(locale)
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, intro: Boolean, withSeconds: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        val verb = if (!intro) "" else if (hour in 2..4) "Jsou " else "Je "
        val rest = when {
            withSeconds -> " $minute ${minutes(minute, true)} a ${time.second} ${seconds(time.second)}"
            minute > 0 -> " a $minute ${minutes(minute, true)}"
            else -> ""
        }
        return "$verb$hour ${hours(hour)}$rest"
    }
}
