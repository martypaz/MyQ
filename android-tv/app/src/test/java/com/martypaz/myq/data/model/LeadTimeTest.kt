package com.martypaz.myq.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadTimeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `says a lead time the way a person would`() {
        assertEquals("5 minutes", formatLeadTime(5))
        assertEquals("45 minutes", formatLeadTime(45))
        assertEquals("1 hour", formatLeadTime(60))
        assertEquals("1 hour 30 minutes", formatLeadTime(90))
        assertEquals("2 hours", formatLeadTime(120))
        assertEquals("12 hours", formatLeadTime(720))
        assertEquals("1 minute", formatLeadTime(1))
    }

    @Test
    fun `offers five minutes through to twelve hours`() {
        assertEquals(5, LEAD_TIME_OPTIONS.first())
        assertEquals(720, LEAD_TIME_OPTIONS.last())
        assertEquals(LEAD_TIME_OPTIONS.sorted(), LEAD_TIME_OPTIONS)
        assertEquals(LEAD_TIME_OPTIONS.distinct(), LEAD_TIME_OPTIONS)
        assertTrue(LEAD_TIME_OPTIONS.contains(Reminder.DEFAULT_LEAD_MINUTES))
    }

    @Test
    fun `fires the stated number of minutes before the programme`() {
        val start = 1785873600_000L
        val reminder = Reminder("id", "Title", "BBC One", start, leadMinutes = 5)
        assertEquals(start - 5 * 60_000L, reminder.fireAtMillis)
    }

    @Test
    fun `a reminder written by an older version keeps its lead time`() {
        val stored = """
            {"programmeId":"tvmaze-1","title":"Gogglebox","channelName":"Channel 4",
             "startMillis":1785873600000,"leadHours":4}
        """.trimIndent()
        val reminder = json.decodeFromString<Reminder>(stored)
        assertEquals(240, reminder.effectiveLeadMinutes)
        assertEquals(1785873600_000L - 240 * 60_000L, reminder.fireAtMillis)
    }

    @Test
    fun `a reminder written by this version reads its own field`() {
        val stored = """
            {"programmeId":"freeview-1","title":"Gogglebox","channelName":"Channel 4",
             "startMillis":1785873600000,"leadMinutes":15}
        """.trimIndent()
        assertEquals(15, json.decodeFromString<Reminder>(stored).effectiveLeadMinutes)
    }

    @Test
    fun `a reminder with neither field falls back to the default`() {
        val stored = """
            {"programmeId":"x","title":"T","channelName":"C","startMillis":1}
        """.trimIndent()
        assertEquals(
            Reminder.DEFAULT_LEAD_MINUTES,
            json.decodeFromString<Reminder>(stored).effectiveLeadMinutes,
        )
    }
}
