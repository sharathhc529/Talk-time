package io.github.sharathhc529.talktime.speech.lang

import io.github.sharathhc529.talktime.speech.ClockTime
import io.github.sharathhc529.talktime.speech.PhraseOptions
import io.github.sharathhc529.talktime.speech.TimePhraseGenerator
import io.github.sharathhc529.talktime.speech.TimeStyle

object Korean : TimePhraseGenerator {

    /** Native Korean numbers in attributive form, used for hours ("한 시", "두 시", ...). */
    private val hours = listOf(
        "제로", "한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열", "열한",
        "열두", "열세", "열네", "열다섯", "열여섯", "열일곱", "열여덟", "열아홉", "스무", "스무한", "스무두", "스무세",
    )

    private val sinoUnits = listOf("영", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")

    /** Sino-Korean numbers, used for minutes and seconds ("이십오 분"). */
    fun sino(n: Int): String {
        if (n < 10) return sinoUnits[n]
        val tens = (if (n / 10 > 1) sinoUnits[n / 10] else "") + "십"
        return tens + if (n % 10 == 0) "" else sinoUnits[n % 10]
    }

    override fun phrase(time: ClockTime, options: PhraseOptions): String =
        if (options.style == TimeStyle.COMMON) common(time, options.introText, options.timeOfDay)
        else formal(time, options.is24Hour, options.seconds, options.introText, options.timeOfDay)

    private fun withDayPart(time: ClockTime, text: String): String {
        if (text.contains("정오") || text.contains("자정")) return text
        val hour = time.dialHour
        return when {
            !time.isPm && (hour in 1..6 || hour == 12) -> "새벽 $text"
            !time.isPm && hour in 7..11 -> "아침 $text"
            time.isPm && hour in 6..11 -> "저녁 $text"
            else -> text
        }
    }

    /** Polite sentence ending: "예요" after a vowel-final word, "이에요" otherwise. */
    private fun sentence(text: String, minute: Int) = if (minute == 0) text + "예요" else text + "이에요"

    private fun common(time: ClockTime, intro: Boolean, timeOfDay: Boolean): String {
        val hour = time.dialHour
        val minute = time.minute
        val next = (if (hour == 12) 0 else hour) + 1
        var text = when {
            minute == 30 -> hours[hour] + "시 반"
            minute in 1..29 -> hours[hour] + "시 " + sino(minute) + "분"
            minute in 31..59 -> hours[next] + "시 " + sino(60 - minute) + "분 전"
            hour == 12 -> if (time.isPm) "정오" else "자정"
            else -> hours[hour] + "시"
        }
        if (timeOfDay) text = withDayPart(time, text)
        return if (intro) sentence(text, minute) else text
    }

    private fun formal(time: ClockTime, is24Hour: Boolean, withSeconds: Boolean, intro: Boolean, timeOfDay: Boolean): String {
        val hour = if (is24Hour) time.hour else time.dialHour
        val minute = time.minute
        var text = hours[hour] + "시"
        if (minute > 0) text += " " + sino(minute) + "분"
        if (withSeconds && time.second != 0) text += " " + sino(time.second) + "초"
        if (timeOfDay && !is24Hour) text = (if (time.isPm) "오후 " else "오전 ") + text
        return if (intro) sentence(text, minute) else text
    }
}
