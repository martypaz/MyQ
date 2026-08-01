package com.martypaz.myq.reminders

import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMigrationTest {

    private val nineOClock = 1785873600_000L

    private fun reminder(
        id: String,
        title: String = "Gogglebox",
        channel: String = "Channel 4",
        start: Long = nineOClock,
        leadHours: Int = 2,
    ) = Reminder(
        programmeId = id, title = title, channelName = channel,
        startMillis = start, leadHours = leadHours,
    )

    private fun programme(
        id: String,
        title: String = "Gogglebox",
        channel: String = "Channel 4",
        start: Long = nineOClock,
    ) = Programme(
        id = id, title = title, channelName = channel,
        platform = Platform.FREEVIEW, startMillis = start,
    )

    @Test
    fun `repoints a reminder whose programme id the listings no longer use`() {
        val remaps = remapReminders(
            reminders = listOf(reminder("tvmaze-3686256")),
            programmes = listOf(programme("freeview-Channel4.uk-$nineOClock")),
        )
        assertEquals(1, remaps.size)
        assertEquals("freeview-Channel4.uk-$nineOClock", remaps.single().to.programmeId)
    }

    @Test
    fun `carries the reminder's own settings across untouched`() {
        val remaps = remapReminders(
            listOf(reminder("tvmaze-1", leadHours = 24)),
            listOf(programme("freeview-1")),
        )
        val moved = remaps.single().to
        assertEquals(24, moved.leadHours)
        assertEquals("Gogglebox", moved.title)
    }

    @Test
    fun `takes the start time from the listing, since the alarm is set from it`() {
        val remaps = remapReminders(
            listOf(reminder("tvmaze-1", start = nineOClock + 60_000)),
            listOf(programme("freeview-1", start = nineOClock)),
        )
        assertEquals(nineOClock, remaps.single().to.startMillis)
    }

    @Test
    fun `leaves reminders alone when the listings still know their id`() {
        assertTrue(
            remapReminders(
                listOf(reminder("freeview-1")),
                listOf(programme("freeview-1")),
            ).isEmpty(),
        )
    }

    @Test
    fun `matches across the channel spellings the two sources use`() {
        val remaps = remapReminders(
            listOf(reminder("tvmaze-1", channel = "5")),
            listOf(programme("freeview-1", channel = "5 HD")),
        )
        assertEquals(1, remaps.size)
    }

    @Test
    fun `tolerates the sources rounding the start time differently`() {
        val remaps = remapReminders(
            listOf(reminder("tvmaze-1", start = nineOClock)),
            listOf(programme("freeview-1", start = nineOClock + 60_000)),
        )
        assertEquals(1, remaps.size)
    }

    @Test
    fun `leaves a reminder alone when nothing in the listings matches it`() {
        assertTrue(
            remapReminders(
                listOf(reminder("tvmaze-1", title = "Cancelled Programme")),
                listOf(programme("freeview-1", title = "Something Else")),
            ).isEmpty(),
        )
    }

    @Test
    fun `does not move a reminder onto a different broadcast of the same series`() {
        val remaps = remapReminders(
            listOf(reminder("tvmaze-1", start = nineOClock)),
            listOf(programme("freeview-1", start = nineOClock + 4L * 3_600_000)),
        )
        assertTrue(remaps.isEmpty())
    }

    @Test
    fun `does not collide two stale reminders onto one new id`() {
        val remaps = remapReminders(
            reminders = listOf(reminder("tvmaze-1"), reminder("tvmaze-2")),
            programmes = listOf(programme("freeview-1")),
        )
        assertEquals(1, remaps.size)
        assertEquals("tvmaze-1", remaps.single().from.programmeId)
    }

    @Test
    fun `is a no-op with nothing to work from`() {
        assertTrue(remapReminders(emptyList(), listOf(programme("freeview-1"))).isEmpty())
        assertTrue(remapReminders(listOf(reminder("tvmaze-1")), emptyList()).isEmpty())
    }

    @Test
    fun `is idempotent, so repeated loads do not churn`() {
        val programmes = listOf(programme("freeview-1"))
        val first = remapReminders(listOf(reminder("tvmaze-1")), programmes)
        val second = remapReminders(first.map { it.to }, programmes)
        assertEquals(1, first.size)
        assertTrue(second.isEmpty())
    }
}
