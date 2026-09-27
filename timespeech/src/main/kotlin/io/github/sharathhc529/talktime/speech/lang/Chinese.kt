package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

/** Mandarin (Mainland China, simplified characters). */
object SimplifiedChinese : ChineseBase(
    hourMark = "点",
    intro = "现在是",
    measureWordMinutes = false,
    dayParts = { hour, minute ->
        when {
            hour <= 4 -> "凌晨"
            hour <= 6 -> "早上"
            hour <= 10 || (hour == 11 && minute <= 30) -> "上午"
            hour == 11 || (hour == 12 && minute <= 30) -> "中午"
            hour <= 18 -> "下午"
            hour <= 22 -> "晚间"
            else -> "晚上"
        }
    },
)

/** Traditional characters, used for Cantonese (Hong Kong) and Taiwan. */
object TraditionalChinese : ChineseBase(
    hourMark = "點",
    intro = "現在是",
    measureWordMinutes = true,
    dayParts = { hour, minute ->
        when {
            hour <= 4 -> "凌晨"
            hour <= 10 || (hour == 11 && minute <= 30) -> "早上"
            hour == 11 || (hour == 12 && minute <= 30) -> "中午"
            hour <= 17 -> "下午"
            hour <= 22 -> "晚间"
            else -> "晚上"
        }
    },
)

abstract class ChineseBase(
    private val hourMark: String,
    private val intro: String,
    /** Whether minute counts use the measure-word form of two ("两") instead of "二". */
    private val measureWordMinutes: Boolean,
    /** Part of the day for a 24-hour time (midnight itself is handled separately). */
    private val dayParts: (hour: Int, minute: Int) -> String,
) : TimePhraseGenerator {

    private val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    fun number(n: Int, measureWord: Boolean): String = when {
        n == 2 && measureWord -> "两"
        n < 10 -> digits[n]
        n == 10 -> "十"
        n < 20 -> "十" + digits[n % 10]
        n % 10 == 0 -> digits[n / 10] + "十"
        else -> digits[n / 10] + "十" + digits[n % 10]
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            val base = common(time)
            if (options.timeOfDay) dayPart(time) + base else base
        } else {
            val base = formal(time, options.is24Hour, options.seconds)
            if (options.timeOfDay && !options.is24Hour) (if (time.isPm) "下午" else "上午") + base else base
        }
        return if (options.introText) intro + text else text
    }

    private fun dayPart(time: ClockTime) =
        if (time.hour == 0 && time.minute == 0) "午夜" else dayParts(time.hour, time.minute)

    private fun minutes(minute: Int) = when (minute) {
        in 1..9 -> number(0, true) + number(minute, measureWordMinutes) + "分"
        else -> number(minute, measureWordMinutes) + "分"
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean): String {
        val minute = time.minute
        val hour = when {
            !is24Hour -> time.dialHour
            time.hour == 0 && minute == 0 -> 24
            else -> time.hour
        }
        var text = number(hour, true) + hourMark
        when {
            minute == 0 && !withSeconds -> text += "整"
            minute > 0 -> text += minutes(minute)
        }
        if (!withSeconds) return text
        return if (minute == 0 && time.second == 0) text + "整" else text + number(time.second, false) + "秒"
    }

    private fun common(time: ClockTime): String {
        val text = number(time.dialHour, true) + hourMark
        return text + when (time.minute) {
            0 -> "整"
            15 -> "一刻"
            30 -> "半"
            45 -> "三刻"
            else -> minutes(time.minute)
        }
    }
}
