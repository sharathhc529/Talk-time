package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Russian : TimePhraseGenerator {
    private val locale = Locale("ru")

    override val supportsSeconds = false

    private enum class Gender { MALE, FEMALE, NEUTER }

    private val units = listOf(
        "ноль", "", "", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять", "десять",
        "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать", "шестнадцать",
        "семнадцать", "восемнадцать", "девятнадцать",
    )
    private val tens = listOf("", "", "двадцать", "тридцать", "сорок", "пятьдесят")

    /** Genitive cardinals ("без пяти ..."). */
    private val genitiveCardinals = listOf(
        "", "одной", "двух", "трёх", "четырёх", "пяти", "шести", "семи", "восьми", "девяти", "десяти",
        "одиннадцати", "двенадцати", "тринадцати", "четырнадцати", "пятнадцати", "шестнадцати", "семнадцати",
        "восемнадцати", "девятнадцати", "двадцати", "двадцати одной", "двадцати двух", "двадцати трёх",
        "двадцати четырёх", "двадцати пяти", "двадцати шести", "двадцати семи", "двадцати восьми", "двадцати девяти",
    )

    /** Genitive ordinals of the next hour ("половина первого"). */
    private val genitiveOrdinals = listOf(
        "", "первого", "второго", "третьего", "четвёртого", "пятого", "шестого", "седьмого", "восьмого",
        "девятого", "десятого", "одиннадцатого", "двенадцатого",
    )

    private fun number(n: Int, gender: Gender): String {
        val unit = n % 10
        fun small(u: Int) = when (u) {
            1 -> when (gender) {
                Gender.MALE -> "один"
                Gender.FEMALE -> "одна"
                Gender.NEUTER -> "одно"
            }
            2 -> if (gender == Gender.FEMALE) "две" else "два"
            else -> units[u]
        }
        return when {
            n < 20 -> small(n)
            unit == 0 -> tens[n / 10]
            else -> tens[n / 10] + " " + small(unit)
        }
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time)
            val special = listOf("полночь", "полдень", "Полночь", "Полдень").any { base.startsWith(it) }
            if (options.timeOfDay && !special) base + dayPart(time) else base
        } else {
            val base = formal(time, options.is24Hour)
            if (options.timeOfDay && !options.is24Hour) base + dayPart(time) else base
        }
        return if (options.introText) "Сейчас $text" else text.capitalizeFirst(locale)
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hourOfHalfDay
        return when {
            time.isPm -> if (hour in 6..11) " вечера" else " дня"
            hour < 6 -> " ночи"
            else -> " утра"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val hourText = when {
            hour == 1 -> "час"
            hour in 2..4 || hour >= 22 -> number(hour, Gender.NEUTER) + " часа"
            hour in 5..20 || hour == 0 -> number(hour, Gender.NEUTER) + " часов"
            else -> number(hour, Gender.MALE) + " час"
        }
        val minute = time.minute
        val unit = minute % 10
        return when {
            minute == 0 -> hourText
            unit == 1 && minute != 11 -> "$hourText ${number(minute, Gender.FEMALE)} минута"
            unit in 2..4 && minute !in 12..14 -> "$hourText ${number(minute, Gender.FEMALE)} минуты"
            minute >= 5 -> "$hourText ${number(minute, Gender.NEUTER)} минут"
            else -> hourText
        }
    }

    private fun minuteWord(n: Int) = when {
        n == 1 || n == 21 -> "минута"
        n in 2..4 || n in 22..24 || n == 39 || n == 59 -> "минуты"
        n in 5..58 -> "минут"
        else -> ""
    }

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val hourOfHalfDay = if (hour == 12) 0 else hour
        val next = hourOfHalfDay + 1
        return when {
            minute == 0 -> when {
                hour == 12 -> if (time.isPm) "полдень" else "полночь"
                hour == 1 -> "час"
                hour in 2..4 -> number(hour, Gender.NEUTER) + " часа"
                else -> number(hour, Gender.NEUTER) + " часов"
            }
            minute == 15 -> "четверть " + genitiveOrdinals[next]
            minute == 45 -> if (hourOfHalfDay == 0) "без четверти час" else "без четверти " + number(next, Gender.NEUTER)
            minute == 30 -> "половина " + genitiveOrdinals[next]
            minute in 1..2 || minute in 21..22 ->
                number(minute, Gender.FEMALE) + " " + minuteWord(minute) + " " + genitiveOrdinals[next]
            minute in 1..29 -> number(minute, Gender.NEUTER) + " " + minuteWord(minute) + " " + genitiveOrdinals[next]
            else -> {
                val nextHour = if (hourOfHalfDay == 0) "час" else number(next, Gender.NEUTER)
                "без " + genitiveCardinals[60 - minute] + " " + minuteWord(minute) + " " + nextHour
            }
        }
    }
}
