package com.martypaz.freeviewguide.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTest {

    @Test
    fun `fires the configured hours before the start`() {
        val hourMillis = 60L * 60L * 1000L
        val start = 100 * hourMillis
        val reminder = Reminder(
            programmeId = "p1", title = "T", channelName = "BBC One",
            startMillis = start, leadHours = 4,
        )
        assertEquals(start - 4 * hourMillis, reminder.fireAtMillis)
    }
}
