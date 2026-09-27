package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Polish : TimePhraseGenerator {
    private val locale = Locale("pl")

    override val supportsSeconds = false

    private val ordinalStems = listOf(
        "zero", "pierwsz", "drug", "trzeci", "czwart", "piąt", "szóst", "siódm", "ósm", "dziewiąt", "dziesiąt",
        "jedenast", "dwunast", "trzynast", "czternast", "piętnast", "szesnast", "siedemnast", "osiemnast",
        "dziewiętnast", "dwudziest", "dwudziesta pierwsz", "dwudziesta drug", "dwudziesta trzeci",
    )

    private val genitives = listOf(
        "", "pierwszej", "drugiej", "trzeciej", "czwartej", "piątej", "szóstej", "siódmej", "ósmej", "dziewiątej",
        "dziesiątej", "jedenastej", "dwunastej", "trzynastej", "czternastej", "piętnastej", "szesnastej",
        "siedemnastej", "osiemnastej", "dziewiętnastej", "dwudziestej", "dwudziestej pierwszej",
        "dwudziestej drugi", "dwudziestej trzeci",
    )

    /** Feminine ordinal, nominative ("pierwsza") or accusative ("pierwszą"). */
    private fun ordinal(n: Int, accusative: Boolean) =
        if (n == 0) "zero" else ordinalStems[n] + if (accusative) "ą" else "a"

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val common = options.style == TimeStyle.COMMON
        var text = if (common) common(time) else formal(time, options.is24Hour)
        val applies = common || !options.is24Hour
        if (options.timeOfDay && applies && !text.endsWith("północ") && !text.endsWith("południe")) {
            val hour = time.hourOfHalfDay
            text += when {
                !common -> if (time.isPm) " popołudniu" else " rano"
                time.isPm -> when (hour) {
                    in 6..9 -> " wieczorem"
                    in 10..11 -> " w nocy"
                    else -> " po południu"
                }
                hour in 0..4 -> " w nocy"
                else -> " rano"
            }
        }
        return if (options.introText) "Jest $text" else text.capitalizeFirst(locale)
    }

    private fun minutes(count: Int) = when {
        count == 1 -> "minuta"
        count in 2..4 || count in 22..24 -> "$count minuty"
        else -> "$count minut"
    }

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = if (hour == 12) 1 else hour + 1
        return when (minute) {
            0 -> when {
                hour != 12 -> ordinal(hour, false) + " godzina"
                time.isPm -> "południe"
                else -> "północ"
            }
            15 -> "kwadrans po " + genitives[hour]
            30 -> "wpół do " + genitives[next]
            45 -> "kwadrans przed " + ordinal(next, true)
            in 1..29 -> minutes(minute) + " po " + genitives[hour]
            else -> minutes(60 - minute) + " przed " + ordinal(next, true)
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        return when {
            time.minute != 0 -> ordinal(hour, false) + " " + time.minute
            hour == 0 -> "północ"
            else -> ordinal(hour, false) + " godzina"
        }
    }
}
