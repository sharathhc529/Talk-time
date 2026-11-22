package io.github.sharathhc529.talktime.speech

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeakingWindowTest {

    private fun at(hour: Int, minute: Int, second: Int = 0, day: Int = 15) =
        LocalDateTime.of(2024, 1, day, hour, minute, second)

    private val morning = SpeakingWindow.ofMinutes(5 * 60, 7 * 60)!!
    private val every5 = SpeakingInterval.EVERY_5_MINUTES

    @Test
    fun `morning schedule includes both ends`() {
        assertTrue(at(5, 0) in morning)
        assertTrue(at(6, 30) in morning)
        assertTrue(at(7, 0) in morning)
        assertFalse(at(7, 0, 15) in morning)
        assertFalse(at(4, 59) in morning)
        assertFalse(at(12, 0) in morning)
    }

    @Test
    fun `starts at 5 and stops a minute after 7`() {
        assertEquals(SpeakingWindow.Change(at(5, 0), starts = true), morning.nextChangeAfter(at(3, 12)))
        assertEquals(SpeakingWindow.Change(at(7, 1), starts = false), morning.nextChangeAfter(at(5, 0)))
        assertEquals(SpeakingWindow.Change(at(7, 1), starts = false), morning.nextChangeAfter(at(6, 59)))
        assertEquals(SpeakingWindow.Change(at(5, 0, day = 16), starts = true), morning.nextChangeAfter(at(7, 1)))
        assertEquals(SpeakingWindow.Change(at(5, 0, day = 16), starts = true), morning.nextChangeAfter(at(18, 55)))
    }

    @Test
    fun `the announcement at the stop time is the last one`() {
        assertTrue(morning.isLastAnnouncement(every5, at(7, 0)))
        assertFalse(morning.isLastAnnouncement(every5, at(6, 55)))
        assertFalse(morning.isLastAnnouncement(every5, at(12, 0)))
        // Hourly with a stop at 6:30: 6:00 is the last one.
        val halfPast = SpeakingWindow.ofMinutes(5 * 60, 6 * 60 + 30)!!
        assertTrue(halfPast.isLastAnnouncement(SpeakingInterval.EVERY_HOUR, at(6, 0)))
        assertTrue(morning.isLastAnnouncement(SpeakingInterval.EVERY_15_SECONDS, at(7, 0)))
    }

    @Test
    fun `schedule over midnight`() {
        val night = SpeakingWindow.ofMinutes(22 * 60, 6 * 60)!!
        assertTrue(at(23, 30) in night)
        assertTrue(at(2, 0) in night)
        assertFalse(at(12, 0) in night)
        assertEquals(SpeakingWindow.Change(at(6, 1, day = 16), starts = false), night.nextChangeAfter(at(23, 0)))
        assertEquals(SpeakingWindow.Change(at(22, 0), starts = true), night.nextChangeAfter(at(6, 1)))
        assertTrue(night.isLastAnnouncement(SpeakingInterval.EVERY_HOUR, at(6, 0, day = 16)))
    }

    @Test
    fun `equal start and stop is no schedule`() {
        assertNull(SpeakingWindow.ofMinutes(8 * 60, 8 * 60))
    }
}
