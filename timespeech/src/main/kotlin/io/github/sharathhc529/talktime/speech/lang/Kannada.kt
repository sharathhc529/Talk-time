package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

object Kannada : TimePhraseGenerator {

    private val numbers = listOf(
        "ಸೊನ್ನೆ", "ಒಂದು", "ಎರಡು", "ಮೂರು", "ನಾಲ್ಕು", "ಐದು", "ಆರು", "ಏಳು", "ಎಂಟು", "ಒಂಬತ್ತು",
        "ಹತ್ತು", "ಹನ್ನೊಂದು", "ಹನ್ನೆರಡು", "ಹದಿಮೂರು", "ಹದಿನಾಲ್ಕು", "ಹದಿನೈದು", "ಹದಿನಾರು", "ಹದಿನೇಳು", "ಹದಿನೆಂಟು", "ಹತ್ತೊಂಬತ್ತು",
        "ಇಪ್ಪತ್ತು", "ಇಪ್ಪತ್ತೊಂದು", "ಇಪ್ಪತ್ತೆರಡು", "ಇಪ್ಪತ್ಮೂರು", "ಇಪ್ಪತ್ನಾಲ್ಕು", "ಇಪ್ಪತ್ತೈದು", "ಇಪ್ಪತ್ತಾರು", "ಇಪ್ಪತ್ತೇಳು", "ಇಪ್ಪತ್ತೆಂಟು", "ಇಪ್ಪತ್ತೊಂಬತ್ತು",
        "ಮೂವತ್ತು", "ಮೂವತ್ತೊಂದು", "ಮೂವತ್ತೆರಡು", "ಮೂವತ್ಮೂರು", "ಮೂವತ್ನಾಲ್ಕು", "ಮೂವತ್ತೈದು", "ಮೂವತ್ತಾರು", "ಮೂವತ್ತೇಳು", "ಮೂವತ್ತೆಂಟು", "ಮೂವತ್ತೊಂಬತ್ತು",
        "ನಲವತ್ತು", "ನಲವತ್ತೊಂದು", "ನಲವತ್ತೆರಡು", "ನಲವತ್ಮೂರು", "ನಲವತ್ನಾಲ್ಕು", "ನಲವತ್ತೈದು", "ನಲವತ್ತಾರು", "ನಲವತ್ತೇಳು", "ನಲವತ್ತೆಂಟು", "ನಲವತ್ತೊಂಬತ್ತು",
        "ಐವತ್ತು", "ಐವತ್ತೊಂದು", "ಐವತ್ತೆರಡು", "ಐವತ್ಮೂರು", "ಐವತ್ನಾಲ್ಕು", "ಐವತ್ತೈದು", "ಐವತ್ತಾರು", "ಐವತ್ತೇಳು", "ಐವತ್ತೆಂಟು", "ಐವತ್ತೊಂಬತ್ತು",
    )

    fun number(n: Int) = numbers[n]

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time)
            if (options.timeOfDay) dayPart(time) + " " + base else base
        } else {
            val base = formal(time, options.is24Hour, options.seconds)
            if (options.timeOfDay && !options.is24Hour) dayPart(time) + " " + base else base
        }
        val withIntro = if (options.introText) "ಸಮಯ ಈಗ $text" else text
        return withIntro.trim()
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hour
        val minute = time.minute
        return when {
            hour == 0 && minute == 0 -> "ಮಧ್ಯರಾತ್ರಿ"
            hour <= 4 -> "ಮುಂಜಾನೆ"
            hour <= 11 -> "ಬೆಳಗ್ಗೆ"
            hour == 12 && minute == 0 -> "ಮಧ್ಯಾಹ್ನ"
            hour <= 16 -> "ಮಧ್ಯಾಹ್ನ"
            hour <= 19 -> "ಸಂಜೆ"
            else -> "ರಾತ್ರಿ"
        }
    }

    private fun hourCommonQuarterPast(hour: Int): String = when (hour) {
        1 -> "ಒಂದೂ ಕಾಲು"
        2 -> "ಎರಡೂ ಕಾಲು"
        3 -> "ಮೂರscale ಕಾಲು".let { "ಮೂರ ಕಾಲು" }
        4 -> "ನಾಲ್ಕೂ ಕಾಲು"
        5 -> "ಐದೂ ಕಾಲು"
        6 -> "ಆರೂ ಕಾಲು"
        7 -> "ಏಳೂ ಕಾಲು"
        8 -> "ಎಂಟೂ ಕಾಲು"
        9 -> "ಒಂಬತ್ತೂ ಕಾಲು"
        10 -> "ಹತ್ತೂ ಕಾಲು"
        11 -> "ಹನ್ನೊಂದೂ ಕಾಲು"
        12 -> "ಹನ್ನೆರಡೂ ಕಾಲು"
        else -> number(hour) + " ಗಂಟೆ ಹದಿನೈದು ನಿಮಿಷ"
    }

    private fun hourCommonHalfPast(hour: Int): String = when (hour) {
        1 -> "ಒಂದೂವರೆ"
        2 -> "ಎರಡೂವರೆ"
        3 -> "ಮೂರವರೆ"
        4 -> "ನಾಲ್ಕೂವರೆ"
        5 -> "ಐದೂವರೆ"
        6 -> "ಆರೂವರೆ"
        7 -> "ಏಳೂವರೆ"
        8 -> "ಎಂಟೂವರೆ"
        9 -> "ಒಂಬತ್ತೂವರೆ"
        10 -> "ಹತ್ತೂವರೆ"
        11 -> "ಹನ್ನೊಂದೂವರೆ"
        12 -> "ಹನ್ನೆರಡೂವರೆ"
        else -> number(hour) + " ಗಂಟೆ ಮೂವತ್ತು ನಿಮಿಷ"
    }

    private fun hourCommonQuarterTo(nextHour: Int): String = when (nextHour) {
        1 -> "ಒಂದಕ್ಕೇ ಕಾಲು"
        2 -> "ಎರಡಕ್ಕೇ ಕಾಲು"
        3 -> "ಮೂರಕ್ಕೇ ಕಾಲು"
        4 -> "ನಾಲ್ಕಕ್ಕೇ ಕಾಲು"
        5 -> "ಐದಕ್ಕೇ ಕಾಲು"
        6 -> "ಆರಕ್ಕೇ ಕಾಲು"
        7 -> "ಏಳಕ್ಕೇ ಕಾಲು"
        8 -> "ಎಂಟಕ್ಕೇ ಕಾಲು"
        9 -> "ಒಂಬತ್ತಕ್ಕೇ ಕಾಲು"
        10 -> "ಹತ್ತಕ್ಕೇ ಕಾಲು"
        11 -> "ಹನ್ನೊಂದಕ್ಕೇ ಕಾಲು"
        12 -> "ಹನ್ನೆರಡಕ್ಕೇ ಕಾಲು"
        else -> number(nextHour) + " ಗಂಟೆಗೆ ಹದಿನೈದು ನಿಮಿಷ"
    }

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1

        return when {
            minute == 0 -> number(hour) + " ಗಂಟೆ"
            minute == 15 -> hourCommonQuarterPast(hour)
            minute == 30 -> hourCommonHalfPast(hour)
            minute == 45 -> hourCommonQuarterTo(next)
            minute in 1..29 -> number(hour) + " ಗಂಟೆ " + number(minute) + " ನಿಮಿಷ"
            else -> number(hour) + " ಗಂಟೆ " + number(minute) + " ನಿಮಿಷ"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean): String {
        val minute = time.minute
        val hour = if (!is24Hour) time.dialHour else time.hour
        var text = if (minute == 0) {
            number(hour) + " ಗಂಟೆ"
        } else {
            number(hour) + " ಗಂಟೆ " + number(minute) + " ನಿಮಿಷ"
        }
        val second = time.second
        if (withSeconds && second != 0) {
            text += " " + number(second) + " ಸೆಕೆಂಡು"
        }
        return text
    }
}
