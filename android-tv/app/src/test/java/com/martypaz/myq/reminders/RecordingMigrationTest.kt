package com.martypaz.myq.reminders

import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.RecordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingMigrationTest {

    private val nineOClock = 1785873600_000L

    private fun recording(
        id: String,
        title: String = "Gogglebox",
        channel: String = "Channel 4",
        start: Long = nineOClock,
        isSeries: Boolean = false,
    ) = RecordEntry(
        programmeId = id, title = title, channelName = channel,
        startMillis = start, isSeries = isSeries,
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
    fun `repoints a recording the listings no longer recognise`() {
        val remaps = remapRecordings(
            listOf(recording("tvmaze-3686256")),
            listOf(programme("freeview-4222-$nineOClock")),
        )
        assertEquals("freeview-4222-$nineOClock", remaps.single().to.programmeId)
    }

    @Test
    fun `keeps the series flag, which is what picks up later episodes`() {
        val remaps = remapRecordings(
            listOf(recording("tvmaze-1", isSeries = true)),
            listOf(programme("freeview-1")),
        )
        assertTrue(remaps.single().to.isSeries)
    }

    @Test
    fun `matches across the channel spellings the sources use`() {
        val remaps = remapRecordings(
            listOf(recording("tvmaze-1", channel = "Channel 4")),
            listOf(programme("freeview-1", channel = "Channel 4 Wales")),
        )
        assertEquals(1, remaps.size)
    }

    @Test
    fun `leaves recordings alone when their id is still known`() {
        assertTrue(
            remapRecordings(
                listOf(recording("freeview-1")),
                listOf(programme("freeview-1")),
            ).isEmpty(),
        )
    }

    @Test
    fun `does not move a recording onto a different showing of the same series`() {
        assertTrue(
            remapRecordings(
                listOf(recording("tvmaze-1", start = nineOClock)),
                listOf(programme("freeview-1", start = nineOClock + 4L * 3_600_000)),
            ).isEmpty(),
        )
    }

    @Test
    fun `does not collide two stale recordings onto one new id`() {
        val remaps = remapRecordings(
            listOf(recording("tvmaze-1"), recording("tvmaze-2")),
            listOf(programme("freeview-1")),
        )
        assertEquals(1, remaps.size)
    }

    @Test
    fun `is idempotent, so repeated loads do not churn`() {
        val programmes = listOf(programme("freeview-1"))
        val first = remapRecordings(listOf(recording("tvmaze-1")), programmes)
        assertTrue(remapRecordings(first.map { it.to }, programmes).isEmpty())
    }

    @Test
    fun `is a no-op with nothing to work from`() {
        assertTrue(remapRecordings(emptyList(), listOf(programme("freeview-1"))).isEmpty())
        assertTrue(remapRecordings(listOf(recording("tvmaze-1")), emptyList()).isEmpty())
    }
}
