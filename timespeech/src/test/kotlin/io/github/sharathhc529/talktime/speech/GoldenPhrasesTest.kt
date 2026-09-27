package io.github.sharathhc529.talktime.speech

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares every language against announcements recorded from the original "Tell Me The Time"
 * app (v1.19.0), with the deliberate fixes described in the README applied.
 */
class GoldenPhrasesTest {

    private data class Case(val language: SpeechLanguage, val time: ClockTime, val options: PhraseOptions, val expected: String)

    private fun cases(): List<Case> {
        val text = javaClass.getResource("/golden-phrases.tsv")!!.readText(Charsets.UTF_8)
        return text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
            val f = line.split('\t')
            val (h, m, s) = f[1].split(':').map(String::toInt)
            Case(
                language = SpeechLanguage.valueOf(f[0]),
                time = ClockTime(h, m, s),
                options = PhraseOptions(
                    is24Hour = f[2].toBoolean(),
                    style = TimeStyle.valueOf(f[3]),
                    timeOfDay = f[4].toBoolean(),
                    introText = f[5].toBoolean(),
                    seconds = f[6].toBoolean(),
                ),
                expected = f.getOrElse(7) { "" },
            )
        }.toList()
    }

    @Test
    fun `every language matches the original app`() {
        val cases = cases()
        assertTrue(cases.size > 1500, "golden table was not loaded")
        val failures = cases.mapNotNull { c ->
            val actual = c.language.phrase(c.time, c.options)
            if (actual == c.expected) null else "${c.language} ${c.time} ${c.options}: expected [${c.expected}] got [$actual]"
        }
        assertEquals(emptyList(), failures)
    }

    @Test
    fun `every language produces text for every minute of the day`() {
        for (language in SpeechLanguage.entries) {
            for (hour in 0..23) for (minute in 0..59) {
                for (style in TimeStyle.entries) {
                    val options = PhraseOptions(is24Hour = false, style = style, timeOfDay = true, introText = false, seconds = false)
                    val phrase = language.phrase(ClockTime(hour, minute), options)
                    assertTrue(phrase.isNotBlank(), "$language $hour:$minute $style is blank")
                }
            }
        }
    }
}
