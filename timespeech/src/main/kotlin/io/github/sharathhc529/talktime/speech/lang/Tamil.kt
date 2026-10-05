package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

object Tamil : TimePhraseGenerator {

    private val numbers = listOf(
        "பூஜ்ஜியம்", "ஒன்று", "இரண்டு", "மூன்று", "நான்கு", "ஐந்து", "ஆறு", "ஏழு", "எட்டு", "ஒன்பது",
        "பத்து", "பதினொன்று", "பன்னிரண்டு", "பதின்மூன்று", "பதினான்கு", "பதினைந்து", "பதினாறு", "பதினேழு", "பதினெட்டு", "பத்தொன்பது",
        "இருபது", "இருபத்தொன்று", "இருபத்திரண்டு", "இருபத்து மூன்று", "இருபத்து நான்கு", "இருபத்தைந்து", "இருபத்தாறு", "இருபத்தேழு", "இருபத்தெட்டு", "இருபத்தொன்பது",
        "முப்பது", "முப்பத்தொன்று", "முப்பத்திரண்டு", "முப்பத்து மூன்று", "முப்பத்து நான்கு", "முப்பத்தைந்து", "முப்பத்தாறு", "முப்பத்தேழு", "முப்பத்தெட்டு", "முப்பத்தொன்பது",
        "நாற்பது", "நாற்பத்தொன்று", "நாற்பத்திரண்டு", "நாற்பத்து மூன்று", "நாற்பத்து நான்கு", "நாற்பத்தைந்து", "நாற்பத்தாறு", "நாற்பத்தேழு", "நாற்பத்தெட்டு", "நாற்பத்தொன்பது",
        "ஐம்பது", "ஐம்பத்தொன்று", "ஐம்பத்திரண்டு", "ஐம்பத்து மூன்று", "ஐம்பத்து நான்கு", "ஐம்பத்தைந்து", "ஐம்பத்தாறு", "ஐம்பத்தேழு", "ஐம்பத்தெட்டு", "ஐம்பத்தொன்பது"
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
        val withIntro = if (options.introText) "இப்போது நேரம் $text" else text
        return withIntro.trim()
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hour
        val minute = time.minute
        return when {
            hour == 0 && minute == 0 -> "நள்ளிரவு"
            hour <= 4 -> "அதிகாலை"
            hour <= 11 -> "காலை"
            hour == 12 && minute == 0 -> "நண்பகல்"
            hour <= 16 -> "மதியம்"
            hour <= 19 -> "மாலை"
            else -> "இரவு"
        }
    }

    private fun hourCommonQuarterPast(hour: Int): String = when (hour) {
        1 -> "ஒன்றேகால்"
        2 -> "இரண்டேகால்"
        3 -> "மூன்றேகால்"
        4 -> "நான்கேகால்"
        5 -> "ஐந்தேகால்"
        6 -> "ஆறேகால்"
        7 -> "ஏழேகால்"
        8 -> "எட்டேகால்"
        9 -> "ஒன்பதேகால்"
        10 -> "பத்தேகால்"
        11 -> "பதினொன்றேகால்"
        12 -> "பன்னிரண்டேகால்"
        else -> number(hour) + " மணி பதினைந்து நிமிடம்"
    }

    private fun hourCommonHalfPast(hour: Int): String = when (hour) {
        1 -> "ஒன்றரை"
        2 -> "இரண்டரை"
        3 -> "மூன்றரை"
        4 -> "நான்கரை"
        5 -> "ஐந்தரை"
        6 -> "ஆறரை"
        7 -> "ஏழரை"
        8 -> "எட்டரை"
        9 -> "ஒன்பதரை"
        10 -> "பத்தரை"
        11 -> "பதினொன்றரை"
        12 -> "பன்னிரண்டரை"
        else -> number(hour) + " மணி முப்பது நிமிடம்"
    }

    private fun hourCommonQuarterTo(nextHour: Int): String = when (nextHour) {
        1 -> "ஒன்றே முக்கால்"
        2 -> "இரண்டே முக்கால்"
        3 -> "மூன்றே முக்கால்"
        4 -> "நான்கே முக்கால்"
        5 -> "ஐந்தே முக்கால்"
        6 -> "ஆறே முக்கால்"
        7 -> "ஏழே முக்கால்"
        8 -> "எட்டே முக்கால்"
        9 -> "ஒன்பதே முக்கால்"
        10 -> "பத்தே முக்கால்"
        11 -> "பதினொன்றே முக்கால்"
        12 -> "பன்னிரண்டே முக்கால்"
        else -> number(nextHour) + " மணிக்கு பதினைந்து நிமிடம்"
    }

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1

        return when {
            minute == 0 -> number(hour) + " மணி"
            minute == 15 -> hourCommonQuarterPast(hour)
            minute == 30 -> hourCommonHalfPast(hour)
            minute == 45 -> hourCommonQuarterTo(next)
            minute in 1..29 -> number(hour) + " மணி " + number(minute) + " நிமிடம்"
            else -> number(hour) + " மணி " + number(minute) + " நிமிடம்"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean): String {
        val minute = time.minute
        val hour = if (!is24Hour) time.dialHour else time.hour
        var text = if (minute == 0) {
            number(hour) + " மணி"
        } else {
            number(hour) + " மணி " + number(minute) + " நிமிடம்"
        }
        val second = time.second
        if (withSeconds && second != 0) {
            text += " " + number(second) + " வினாடி"
        }
        return text
    }
}
