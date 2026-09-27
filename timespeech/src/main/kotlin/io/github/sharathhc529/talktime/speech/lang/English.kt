package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle
import io.github.sharathhc529.talktime.speech.capitalizeFirst
import java.util.Locale

object English : TimePhraseGenerator {
    private val locale = Locale.ENGLISH

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val hour = time.dialHour
            val base = common(time, hour, options.timeOfDay)
            when {
                !options.timeOfDay || base.endsWith("midnight") || base.endsWith("noon") -> base
                time.isPm -> when (hour) {
                    in 6..10 -> "$base in the evening"
                    11 -> "$base at night"
                    else -> "$base in the afternoon"
                }
                hour in 1..4 || (hour == 12 && time.minute > 0) -> "$base at night"
                else -> "$base in the morning"
            }
        } else {
            val base = if (options.seconds) withSeconds(time, options.is24Hour) else formal(time, options.is24Hour, options.timeOfDay)
            if (!options.is24Hour && options.timeOfDay) base + if (time.isPm) " PM" else " AM" else base
        }
        return if (options.introText) "It's $text" else text.capitalizeFirst(locale)
    }

    /** Names 12 o'clock as noon or midnight; [after] is true when the phrase is "... past <hour>". */
    private fun hourName(hour: Int, isPm: Boolean, after: Boolean): String = when {
        hour == 12 && isPm -> if (after) "noon" else "midnight"
        hour == 12 -> if (after) "midnight" else "noon"
        else -> hour.toString()
    }

    private fun common(time: ClockTime, dialHour: Int, timeOfDay: Boolean): String {
        val minute = time.minute
        val pm = time.isPm
        val next = (if (dialHour == 12) 0 else dialHour) + 1
        return when (minute) {
            15 -> "quarter past " + hourName(dialHour, pm, true)
            45 -> "quarter to " + hourName(next, pm, false)
            30 -> "half past " + hourName(dialHour, pm, true)
            in 1..29 -> minutesPhrase(minute) + " past " + hourName(dialHour, pm, true)
            in 31..59 -> minutesPhrase(60 - minute) + " to " + hourName(next, pm, false)
            else -> when {
                pm && dialHour == 12 -> "noon"
                dialHour == 12 -> "midnight"
                timeOfDay -> dialHour.toString()
                else -> "$dialHour o'clock"
            }
        }
    }

    private fun minutesPhrase(minutes: Int) = when {
        minutes % 5 == 0 -> "$minutes"
        minutes == 1 -> "1 minute"
        else -> "$minutes minutes"
    }

    private fun withSeconds(time: ClockTime, is24Hour: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val unit = if (time.second == 1) "second" else "seconds"
        val minute = if (time.minute in 1..9) "O ${time.minute}" else "${time.minute}"
        return "$hour $minute and ${time.second} $unit"
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, timeOfDay: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        // With "PM" appended, 12 is spelled out so that "12 PM" is not misread by the TTS engine.
        val spellNoon = !is24Hour && time.isPm && hour == 12 && timeOfDay
        return when {
            minute in 1..9 && spellNoon -> "twelve O $minute"
            minute in 1..9 && !is24Hour -> "$hour O $minute"
            minute == 0 && !is24Hour && !timeOfDay -> "$hour o'clock"
            minute == 0 && is24Hour -> when {
                hour == 0 -> "zero hundred hours"
                hour < 10 -> "zero $hour hundred hours"
                else -> "$hour hundred hours"
            }
            !is24Hour -> when {
                minute > 0 && spellNoon -> "twelve $minute"
                minute == 0 -> if (spellNoon) "twelve" else hour.toString()
                else -> "${time.dialHour} %02d".format(minute)
            }
            else -> {
                val hourText = when {
                    hour == 0 -> "zero zero"
                    hour < 10 -> "zero $hour"
                    else -> hour.toString()
                }
                if (minute < 10) "$hourText zero $minute" else "$hourText $minute"
            }
        }
    }
}
