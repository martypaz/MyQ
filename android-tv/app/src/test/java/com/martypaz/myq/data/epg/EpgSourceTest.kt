package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgSourceTest {

    private val nineOClock = 1785873600_000L

    private fun programme(
        id: String,
        title: String = "Gogglebox",
        channel: String = "Channel 4",
        start: Long = nineOClock,
        synopsis: String = "",
        genres: List<String> = emptyList(),
        newness: Newness = Newness.NONE,
    ) = Programme(
        id = id, title = title, synopsis = synopsis, channelName = channel,
        platform = Platform.FREEVIEW, genres = genres, startMillis = start,
        newness = newness,
    )

    @Test
    fun `the first source owns identity`() {
        val merged = mergeAll(
            listOf(
                listOf(programme("freeview-1")),
                listOf(programme("tuner-1")),
                listOf(programme("tvmaze-1")),
            ),
        )
        assertEquals("freeview-1", merged.single().id)
    }

    @Test
    fun `every source contributes what only it knows`() {
        val merged = mergeAll(
            listOf(
                listOf(programme("freeview-1")),
                listOf(programme("tuner-1", synopsis = "From the broadcaster")),
                listOf(programme("tvmaze-1", genres = listOf("Reality"), newness = Newness.NEW_SERIES)),
            ),
        ).single()

        assertEquals("From the broadcaster", merged.synopsis)
        assertEquals(listOf("Reality"), merged.genres)
        assertEquals(Newness.NEW_SERIES, merged.newness)
    }

    @Test
    fun `an earlier source is never overwritten by a later one`() {
        val merged = mergeAll(
            listOf(
                listOf(programme("freeview-1", synopsis = "From the feed")),
                listOf(programme("tuner-1", synopsis = "From the tuner")),
            ),
        )
        assertEquals("From the feed", merged.single().synopsis)
    }

    @Test
    fun `a later source can still add programmes the first never had`() {
        val merged = mergeAll(
            listOf(
                listOf(programme("freeview-1", title = "Gogglebox")),
                listOf(programme("tuner-1", title = "Regional News", start = nineOClock + 7_200_000)),
            ),
        )
        assertEquals(listOf("Gogglebox", "Regional News"), merged.map { it.title })
    }

    @Test
    fun `sources that answered with nothing are skipped`() {
        val merged = mergeAll(
            listOf(emptyList(), listOf(programme("tuner-1")), emptyList()),
        )
        assertEquals("tuner-1", merged.single().id)
    }

    @Test
    fun `no sources at all yields no guide rather than an error`() {
        assertTrue(mergeAll(emptyList()).isEmpty())
        assertTrue(mergeAll(listOf(emptyList(), emptyList())).isEmpty())
    }

    @Test
    fun `merge order matches the declared source order`() {
        // The enum order is load-bearing: it is what mergeAll folds along.
        assertEquals(
            listOf(
                EpgRepository.Source.FREEVIEW_UK,
                EpgRepository.Source.FREEVIEW_EPG,
                EpgRepository.Source.DEVICE_TUNER,
                EpgRepository.Source.TVMAZE,
            ),
            EpgRepository.Source.entries.toList(),
        )
    }

    @Test
    fun `a programme is on now between its start and its end`() {
        val programme = programme("x", start = nineOClock).copy(runtimeMinutes = 60)
        assertTrue(programme.isOnNow(nineOClock))
        assertTrue(programme.isOnNow(nineOClock + 59 * 60_000L))
        assertTrue(!programme.isOnNow(nineOClock - 1))
        assertTrue(!programme.isOnNow(nineOClock + 60 * 60_000L))
    }

    @Test
    fun `an unknown runtime is assumed to be an hour`() {
        val programme = programme("x", start = nineOClock)
        assertTrue(programme.isOnNow(nineOClock + 59 * 60_000L))
        assertTrue(!programme.isOnNow(nineOClock + 61 * 60_000L))
    }
}
