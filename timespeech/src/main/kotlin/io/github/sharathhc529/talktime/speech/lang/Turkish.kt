package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object Turkish : TimePhraseGenerator {
    private val locale = Locale("tr")

    override val supportsSeconds = false

    private val units = listOf("", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz")
    private val tens = listOf("", "on", "yirmi", "otuz", "kırk", "elli")

    /** Accusative hour names, used with "geçiyor" (past). */
    private val accusative = listOf(
        "", "biri", "ikiyi", "üçü", "dördü", "beşi", "altıyı", "yediyi", "sekizi", "dokuzu", "onu", "on biri", "on ikiyi",
    )

    /** Dative hour names, used with "var" (to). */
    private val dative = listOf(
        "", "bire", "ikiye", "üçe", "dörde", "beşe", "altıya", "yediye", "sekize", "dokuza", "ona", "on bire", "on ikiye",
    )

    fun number(n: Int): String = when {
        n == 0 -> "sıfır"
        n < 10 -> units[n]
        n % 10 == 0 -> tens[n / 10]
        else -> tens[n / 10] + " " + units[n % 10]
    }

    private val noonOrMidnight = listOf("Gece yarısı oldu", "Öğlen", "Gece yarısı", "Bu öğlen")

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val common = options.style == TimeStyle.COMMON
        val text = if (common) common(time, options.introText) else formal(time, options.is24Hour, options.introText)
        if (!options.timeOfDay || !(common || !options.is24Hour)) return text.capitalizeFirst(locale)
        if (noonOrMidnight.any { text.endsWith(it) }) return text
        val hour = time.hourOfHalfDay
        val prefix = when {
            !common -> if (time.isPm) "Öğleden sonra " else "Sabah "
            time.isPm -> when (hour) {
                in 6..10 -> "Akşam "
                11 -> "Gece "
                else -> "Öğleden sonra "
            }
            hour in 1..4 -> "Gece "
            else -> "Sabah "
        }
        return prefix + text.lowercase(locale)
    }

    private fun common(time: ClockTime, intro: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = if (hour == 12) 1 else hour + 1
        val saat = if (intro) "Saat " else ""
        return when (minute) {
            0 -> when {
                hour != 12 -> saat + number(hour)
                time.isPm -> if (intro) "Bu öğlen" else "Öğlen"
                else -> if (intro) "Gece yarısı oldu" else "Gece yarısı"
            }
            15 -> saat + accusative[hour] + " çeyrek geçiyor"
            30 -> if (hour == 12 && !time.isPm) saat + "yarım" else saat + number(hour) + " buçuk"
            45 -> saat + dative[next] + " çeyrek var"
            in 1..29 -> saat + accusative[hour] + " " + number(minute) + " dakika geçiyor"
            else -> saat + dative[next] + " " + number(60 - minute) + " dakika var"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, intro: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        val saat = if (intro) "Saat " else ""
        return when {
            minute == 0 && hour == 0 -> if (intro) "Gece yarısı oldu" else "Gece yarısı"
            minute == 0 -> saat + number(hour)
            minute in 1..9 -> saat + number(hour) + " sıfır " + number(minute)
            else -> saat + number(hour) + " " + number(minute)
        }
    }
}
