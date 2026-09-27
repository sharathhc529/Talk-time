package io.github.sharathhc529.talktime.speech

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PhraseExamplesTest {

    private fun options(
        is24Hour: Boolean = false,
        style: TimeStyle = TimeStyle.COMMON,
        timeOfDay: Boolean = false,
        intro: Boolean = true,
        seconds: Boolean = false,
    ) = PhraseOptions(is24Hour, style, timeOfDay, intro, seconds)

    @Test
    fun english() {
        val en = SpeechLanguage.EN_US
        assertEquals("It's quarter past 3", en.phrase(ClockTime(15, 15), options()))
        assertEquals("It's 20 to 5 in the afternoon", en.phrase(ClockTime(16, 40), options(timeOfDay = true)))
        assertEquals("It's 7 minutes to midnight", en.phrase(ClockTime(23, 53), options()))
        assertEquals("It's 14 30", en.phrase(ClockTime(14, 30), options(is24Hour = true, style = TimeStyle.FORMAL)))
        assertEquals("It's 3 O 5 PM", en.phrase(ClockTime(15, 5), options(style = TimeStyle.FORMAL, timeOfDay = true)))
    }

    @Test
    fun german() {
        val de = SpeechLanguage.DE_DE
        assertEquals("Es ist halb 4", de.phrase(ClockTime(15, 30), options()))
        assertEquals("Es ist 5 vor halb 8 abends", de.phrase(ClockTime(19, 25), options(timeOfDay = true)))
    }

    @Test
    fun `dutch has a space before the part of the day`() {
        assertEquals(
            "Het is kwart over drie 's namiddags",
            SpeechLanguage.NL_NL.phrase(ClockTime(15, 15), options(timeOfDay = true)),
        )
    }

    @Test
    fun `seconds are ignored by languages without seconds support`() {
        val withSeconds = options(style = TimeStyle.FORMAL, is24Hour = true, seconds = true)
        val withoutSeconds = withSeconds.copy(seconds = false)
        assertEquals(
            SpeechLanguage.PL_PL.phrase(ClockTime(10, 20, 33), withoutSeconds),
            SpeechLanguage.PL_PL.phrase(ClockTime(10, 20, 33), withSeconds),
        )
    }

    @Test
    fun `device locales map to languages`() {
        assertEquals(SpeechLanguage.EN_GB, SpeechLanguage.forLocale(Locale.UK))
        assertEquals(SpeechLanguage.EN_US, SpeechLanguage.forLocale(Locale("xx")))
        assertEquals(SpeechLanguage.PT_BR, SpeechLanguage.forLocale(Locale("pt", "BR")))
        assertEquals(SpeechLanguage.ZH_TW, SpeechLanguage.forLocale(Locale.TRADITIONAL_CHINESE))
        assertEquals(SpeechLanguage.ID_ID, SpeechLanguage.forLocale(Locale("in", "ID")))
    }
}
