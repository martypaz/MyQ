package com.martypaz.myq.ui

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingTest {

    private fun programme(season: Int?, episode: Int?) = Programme(
        id = "p", title = "T", channelName = "BBC One", platform = Platform.FREEVIEW,
        startMillis = 0L, season = season, episode = episode, newness = Newness.NONE,
    )

    @Test
    fun `season and episode formats like Sky Q`() {
        assertEquals("(S6 Ep1)", formatSeasonEpisode(programme(season = 6, episode = 1)))
    }

    @Test
    fun `episode only still formats`() {
        assertEquals("(Ep3)", formatSeasonEpisode(programme(season = null, episode = 3)))
    }

    @Test
    fun `unknown metadata yields nothing`() {
        assertNull(formatSeasonEpisode(programme(season = null, episode = null)))
    }

    @Test
    fun `imminent start renders as a relative day with a time`() {
        val label = formatStart(System.currentTimeMillis() + 60_000)
        assertTrue(label, label.startsWith("Today") || label.startsWith("Tomorrow"))
        assertTrue(label, Regex("\\d{2}:\\d{2}$").containsMatchIn(label))
    }

    @Test
    fun `short season and episode formats as S01 E01`() {
        assertEquals("S01 E01", formatSeasonEpisodeShort(programme(season = 1, episode = 1)))
        assertEquals("S06 E03", formatSeasonEpisodeShort(programme(season = 6, episode = 3)))
        assertEquals("E03", formatSeasonEpisodeShort(programme(season = null, episode = 3)))
        assertEquals("S02", formatSeasonEpisodeShort(programme(season = 2, episode = null)))
        assertNull(formatSeasonEpisodeShort(programme(season = null, episode = null)))
    }
}
