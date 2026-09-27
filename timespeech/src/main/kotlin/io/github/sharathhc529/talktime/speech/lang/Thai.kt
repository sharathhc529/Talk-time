package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

/** Thai, using the traditional six-hour clock for the common style. */
object Thai : TimePhraseGenerator {
    private const val INTRO = "ขณะนี้เวลา"

    private val units = listOf("ศูนย์", "หนึ่ง", "สอง", "สาม", "สี่", "ห้า", "หก", "เจ็ด", "แปด", "เก้า")

    fun number(n: Int): String {
        if (n < 10) return units[n]
        val tens = when (n / 10) {
            1 -> "สิบ"
            2 -> "ยี่สิบ"
            else -> units[n / 10] + "สิบ"
        }
        return tens + when (n % 10) {
            0 -> ""
            1 -> "เอ็ด"
            else -> units[n % 10]
        }
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String {
        val text = if (options.style == TimeStyle.COMMON) {
            common(time, options.introText, options.timeOfDay)
        } else {
            val base = formal(time, options.is24Hour, options.seconds, options.introText)
            if (options.timeOfDay && !options.is24Hour) base + if (time.isPm) "หลังเที่ยง" else "ก่อนเที่ยง" else base
        }
        return text.trim()
    }

    /** Adds morning/afternoon/evening words to an hour phrase. */
    private fun withDayPart(time: ClockTime, text: String): String {
        val hour = time.dialHour
        return when {
            !time.isPm && hour in 6..11 -> text + "เช้า"
            time.isPm && hour in 1..5 -> "บ่าย$text"
            time.isPm && hour == 6 -> text + "เย็น"
            else -> text
        }
    }

    private fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        var text = if (!time.isPm) when (hour) {
            12 -> "เที่ยงคืน"
            in 1..5 -> "ตี" + number(hour)
            else -> (number(hour) + "โมง").let { if (timeOfDay) withDayPart(time, it) else it }
        } else when (hour) {
            12 -> if (minute == 0) "เที่ยงวัน" else "เที่ยง"
            in 1..6 -> (if (hour == 1) "โมง" else number(hour) + "โมง").let { if (timeOfDay) withDayPart(time, it) else it }
            else -> number(hour - 6) + "ทุ่ม"
        }
        if (minute == 30) text += "ครึ่ง" else if (minute > 0) text += number(minute) + "นาที"
        return if (intro) INTRO + text else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, intro: Boolean): String {
        val hour = when {
            !is24Hour -> time.dialHour
            time.hour == 0 -> 24
            else -> time.hour
        }
        var text = number(hour) + "นาฬิกา"
        if (time.minute > 0) text += number(time.minute) + "นาที"
        if (withSeconds && time.second != 0) text += number(time.second) + "วินาที"
        return if (intro) INTRO + text else text
    }
}
