package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EpgMergeTest {

    private val nineOClock = 1785873600_000L

    private fun programme(
        id: String,
        title: String = "Gogglebox",
        channel: String = "BBC One",
        start: Long = nineOClock,
        synopsis: String = "",
        genres: List<String> = emptyList(),
        runtime: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        newness: Newness = Newness.NONE,
        imageUrl: String? = null,
    ) = Programme(
        id = id, title = title, synopsis = synopsis, channelName = channel,
        platform = Platform.FREEVIEW, genres = genres, startMillis = start,
        runtimeMinutes = runtime, season = season, episode = episode,
        newness = newness, imageUrl = imageUrl,
    )

    @Test
    fun `keeps programmes only one source knows about`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", title = "Teleshopping", start = nineOClock)),
            enrichment = listOf(programme("tv-1", title = "Orbital", start = nineOClock + 3_600_000)),
        )
        assertEquals(listOf("Teleshopping", "Orbital"), merged.map { it.title })
    }

    @Test
    fun `collapses the same broadcast described by both sources`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1")),
            enrichment = listOf(programme("tv-1")),
        )
        assertEquals(1, merged.size)
    }

    @Test
    fun `the schedule owns identity, so stored reminders keep matching`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1")),
            enrichment = listOf(programme("tv-1")),
        )
        assertEquals("fv-1", merged.single().id)
    }

    @Test
    fun `takes genres from TVmaze, which the XMLTV feed does not carry`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1")),
            enrichment = listOf(programme("tv-1", genres = listOf("Comedy", "Reality"))),
        )
        assertEquals(listOf("Comedy", "Reality"), merged.single().genres)
    }

    @Test
    fun `takes newness from TVmaze, which knows premiere dates`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", newness = Newness.NONE)),
            enrichment = listOf(programme("tv-1", newness = Newness.NEW_SERIES)),
        )
        assertEquals(Newness.NEW_SERIES, merged.single().newness)
    }

    @Test
    fun `never overwrites what the schedule already knows`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", synopsis = "From the broadcaster", runtime = 60)),
            enrichment = listOf(programme("tv-1", synopsis = "From TVmaze", runtime = 45)),
        )
        assertEquals("From the broadcaster", merged.single().synopsis)
        assertEquals(60, merged.single().runtimeMinutes)
    }

    @Test
    fun `fills blanks the schedule left empty`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", synopsis = "", imageUrl = null)),
            enrichment = listOf(programme("tv-1", synopsis = "A synopsis", imageUrl = "https://example.test/a.jpg")),
        )
        assertEquals("A synopsis", merged.single().synopsis)
        assertEquals("https://example.test/a.jpg", merged.single().imageUrl)
    }

    @Test
    fun `matches across the channel-name spellings the two sources use`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", channel = "5 HD")),
            enrichment = listOf(programme("tv-1", channel = "5", genres = listOf("Documentary"))),
        )
        assertEquals(1, merged.size)
        assertEquals(listOf("Documentary"), merged.single().genres)
    }

    @Test
    fun `tolerates sources rounding the start time differently`() {
        val merged = mergeProgrammes(
            schedule = listOf(programme("fv-1", start = nineOClock)),
            enrichment = listOf(programme("tv-1", start = nineOClock + 60_000, genres = listOf("Comedy"))),
        )
        assertEquals(1, merged.size)
        assertEquals(listOf("Comedy"), merged.single().genres)
    }

    @Test
    fun `does not collapse different programmes in the same slot on different channels`() {
        val merged = mergeProgrammes(
            schedule = listOf(
                programme("fv-1", title = "The News", channel = "BBC One"),
                programme("fv-2", title = "The News", channel = "ITV1"),
            ),
            enrichment = emptyList(),
        )
        assertEquals(2, merged.size)
    }

    @Test
    fun `does not collapse the same series airing twice in a day`() {
        val merged = mergeProgrammes(
            schedule = listOf(
                programme("fv-1", start = nineOClock),
                programme("fv-2", start = nineOClock + 4L * 3_600_000),
            ),
            enrichment = emptyList(),
        )
        assertEquals(2, merged.size)
        assertNotEquals(merged[0].id, merged[1].id)
    }

    @Test
    fun `returns everything in start order`() {
        val merged = mergeProgrammes(
            schedule = listOf(
                programme("fv-late", title = "Late", start = nineOClock + 7_200_000),
                programme("fv-early", title = "Early", start = nineOClock),
            ),
            enrichment = listOf(programme("tv-mid", title = "Mid", start = nineOClock + 3_600_000)),
        )
        assertEquals(listOf("Early", "Mid", "Late"), merged.map { it.title })
    }

    @Test
    fun `either source being empty is survivable`() {
        val scheduleOnly = mergeProgrammes(listOf(programme("fv-1")), emptyList())
        val enrichmentOnly = mergeProgrammes(emptyList(), listOf(programme("tv-1")))
        assertEquals("fv-1", scheduleOnly.single().id)
        assertEquals("tv-1", enrichmentOnly.single().id)
        assertEquals(emptyList<Programme>(), mergeProgrammes(emptyList(), emptyList()))
    }
}
