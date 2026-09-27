package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

object Hindi : TimePhraseGenerator {

    /** Hindi numbers 0-59 are irregular, so they are listed explicitly. */
    private val numbers = listOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छः", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पन्द्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस",
        "बीस", "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस",
        "तीस", "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस",
        "चालीस", "इकतालीस", "बयालीस", "तैंतालीस", "चौंतालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास",
        "पचास", "इक्याबन", "बावन", "तिरेपन", "चौबन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ",
    )

    fun number(n: Int) = numbers[n]

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time, options.introText)
            if (options.timeOfDay) dayPart(time) + " " + base else base
        } else {
            val base = formal(time, options.is24Hour, options.seconds, options.introText)
            if (options.timeOfDay && !options.is24Hour) (if (time.isPm) "शाम के " else "सुबह के ") + base else base
        }
        return text.trim()
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hour
        val minute = time.minute
        return when {
            hour == 0 && minute == 0 -> ""
            hour <= 4 -> "रात"
            hour <= 11 -> "सुबह"
            hour == 12 && minute == 0 -> ""
            hour <= 17 -> "दोपहर"
            hour <= 22 -> "शाम"
            else -> "रात"
        }
    }

    private fun common(time: ClockTime, intro: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        var verb = if (hour == 1) "है" else "हैं"
        val hourText = number(hour) + if (hour == 1) " बजा" else " बजे"
        val text = when {
            minute == 30 -> when (hour) {
                1 -> "डेढ़ बजा"
                2 -> "ढाई बजे"
                else -> "साढ़े $hourText"
            }
            minute == 15 -> "सवा $hourText"
            minute in 1..29 -> number(hour) + " बजकर " + number(minute) + " मिनट हो गए"
            minute in 31..59 -> {
                val next = (if (hour == 12) 0 else hour) + 1
                if (next == 1) verb = "है"
                when {
                    minute != 45 -> number(next) + " बजने में " + number(60 - minute) + " बाकी"
                    next == 1 -> "पौने " + number(next) + " बजा"
                    else -> "पौने " + number(next) + " बजे"
                }
            }
            hour == 12 -> {
                verb = "है"
                if (time.isPm) "दोपहर" else "आधी रात"
            }
            else -> hourText
        }
        return if (intro) "$text $verb" else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, intro: Boolean): String {
        val minute = time.minute
        val hour = when {
            !is24Hour -> time.dialHour
            time.hour == 0 && minute == 0 -> 24
            else -> time.hour
        }
        val verb = if (hour == 1) "है" else "हैं"
        var text = when {
            minute == 0 -> number(hour) + if (hour == 1) " बजा" else " बजे"
            else -> number(hour) + (if (hour == 1) " बजाकर " else " बजकर ") + number(minute) +
                if (minute == 1) " मिनट" else " मिनट्स"
        }
        val second = time.second
        if (withSeconds && minute != 0 && second != 0) {
            text += " और " + number(second) + if (second == 1) " सेकंड" else " सेकण्ड"
        }
        return if (intro) "$text $verb" else text
    }
}
