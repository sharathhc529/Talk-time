package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

object Telugu : TimePhraseGenerator {

    private val numbers = listOf(
        "సున్నా", "ఒకటి", "రెండు", "మూడు", "నాలుగు", "ఐదు", "ఆరు", "ఏడు", "ఎనిమిది", "తొమ్మిది",
        "పది", "పదకొండు", "పన్నెండు", "పదమూడు", "పద్నాలుగు", "పదిహేను", "పదహారు", "పదిహేడు", "పద్దెనిమిది", "పంతొమ్మిది",
        "ఇరవై", "ఇరవై ఒకటి", "ఇరవై రెండు", "ఇరవై మూడు", "ఇరవై నాలుగు", "ఇరవై ఐదు", "ఇరవై ఆరు", "ఇరవై ఏడు", "ఇరవై ఎనిమిది", "ఇరవై తొమ్మిది",
        "ముప్పై", "ముప్పై ఒకటి", "ముప్పై రెండు", "ముప్పై మూడు", "ముప్పై నాలుగు", "ముప్పై ఐదు", "ముప్పై ఆరు", "ముప్పై ఏడు", "ముప్పై ఎనిమిది", "ముప్పై తొమ్మిది",
        "నలభై", "నలభై ఒకటి", "నలభై రెండు", "నలభై మూడు", "నలభై నాలుగు", "నలభై ఐదు", "నలభై ఆరు", "నలభై ఏడు", "నలభై ఎనిమిది", "నలభై తొమ్మిది",
        "యాభై", "యాభై ఒకటి", "యాభై రెండు", "యాభై మూడు", "యాభై నాలుగు", "యాభై ఐదు", "యాభై ఆరు", "యాభై ఏడు", "యాభై ఎనిమిది", "యాభై తొమ్మిది"
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
        val withIntro = if (options.introText) "ఇప్పుడు సమయం $text" else text
        return withIntro.trim()
    }

    private fun dayPart(time: ClockTime): String {
        val hour = time.hour
        val minute = time.minute
        return when {
            hour == 0 && minute == 0 -> "అర్ధరాత్రి"
            hour <= 4 -> "తెల్లవారుజామున"
            hour <= 11 -> "ఉదయం"
            hour == 12 && minute == 0 -> "మధ్యాహ్నం"
            hour <= 16 -> "మధ్యాహ్నం"
            hour <= 19 -> "సాయంత్రం"
            else -> "రాత్రి"
        }
    }

    private fun hourCommonQuarterPast(hour: Int): String = when (hour) {
        1 -> "ఒకటింబావు"
        2 -> "రెండుంబావు"
        3 -> "మూడుంబావు"
        4 -> "నాలుగుంబావు"
        5 -> "ఐదుంబావు"
        6 -> "ఆరుంబావు"
        7 -> "ఏడుంబావు"
        8 -> "ఎనిమిదింబావు"
        9 -> "తొమ్మిదింబావు"
        10 -> "పదింబావు"
        11 -> "పదకొండుంబావు"
        12 -> "పన్నెండుంబావు"
        else -> number(hour) + " గంటల పదిహేను నిమిషాలు"
    }

    private fun hourCommonHalfPast(hour: Int): String = when (hour) {
        1 -> "ఒకటిన్నర"
        2 -> "రెండున్నర"
        3 -> "మూడున్నర"
        4 -> "నాలుగున్నర"
        5 -> "ఐదున్నర"
        6 -> "ఆరున్నర"
        7 -> "ఏడున్నర"
        8 -> "ఎనిమిదిన్నర"
        9 -> "తొమ్మిదిన్నర"
        10 -> "పదిన్నర"
        11 -> "పదకొండున్నర"
        12 -> "పన్నెండున్నర"
        else -> number(hour) + " గంటల ముప్పై నిమిషాలు"
    }

    private fun hourCommonQuarterTo(nextHour: Int): String =
        "పావు తక్కువ " + number(nextHour)

    private fun common(time: ClockTime): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1

        return when {
            minute == 0 -> number(hour) + " గంటలు"
            minute == 15 -> hourCommonQuarterPast(hour)
            minute == 30 -> hourCommonHalfPast(hour)
            minute == 45 -> hourCommonQuarterTo(next)
            minute in 1..29 -> number(hour) + " గంటల " + number(minute) + " నిమిషాలు"
            else -> number(hour) + " గంటల " + number(minute) + " నిమిషాలు"
        }
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean): String {
        val minute = time.minute
        val hour = if (!is24Hour) time.dialHour else time.hour
        var text = if (minute == 0) {
            number(hour) + " గంటలు"
        } else {
            number(hour) + " గంటల " + number(minute) + " నిమిషాలు"
        }
        val second = time.second
        if (withSeconds && second != 0) {
            text += " " + number(second) + " సెకన్లు"
        }
        return text
    }
}
