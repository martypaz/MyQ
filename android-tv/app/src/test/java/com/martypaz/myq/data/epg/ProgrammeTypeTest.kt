package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammeTypeTest {

    private fun programme(
        title: String,
        channel: String = "BBC Two",
        runtime: Int? = 120,
        season: Int? = null,
        episode: Int? = null,
    ) = Programme(
        id = title, title = title, channelName = channel, platform = Platform.FREEVIEW,
        startMillis = 1_000L, runtimeMinutes = runtime, season = season, episode = episode,
    )

    @Test
    fun `a long unnumbered programme is a film`() {
        assertTrue(programme("Women Talking", runtime = 95).isLikelyFilm())
        assertTrue(programme("Captain Phillips", channel = "Film4", runtime = 160).isLikelyFilm())
    }

    @Test
    fun `anything with series numbering is not a film`() {
        assertFalse(programme("Vera", runtime = 110, season = 5, episode = 3).isLikelyFilm())
        assertFalse(programme("Inspector Morse", runtime = 130, season = 4, episode = 2).isLikelyFilm())
    }

    @Test
    fun `shopping and continuity filler is not a film, however long the slot`() {
        assertFalse(programme("Teleshopping", runtime = 120).isLikelyFilm())
        assertFalse(programme("Teleshopping", channel = "Film4", runtime = 240).isLikelyFilm())
        assertFalse(programme(".programmes start at 7.00am", runtime = 271).isLikelyFilm())
        assertFalse(programme("This is BBC Two", runtime = 230).isLikelyFilm())
    }

    @Test
    fun `a short programme is not a film`() {
        assertFalse(programme("The Hit List", runtime = 45).isLikelyFilm())
        assertFalse(programme("5 News Weekend", runtime = 5).isLikelyFilm())
    }

    @Test
    fun `an all-morning magazine show is too long to be a film`() {
        assertFalse(programme("This Morning", runtime = 210).isLikelyFilm())
        assertFalse(programme("James Martin's Saturday Morning", runtime = 230).isLikelyFilm())
    }

    @Test
    fun `unknown runtime is not guessed at`() {
        assertFalse(programme("Something", runtime = null).isLikelyFilm())
    }

    @Test
    fun `a long film survives on a general channel`() {
        assertTrue(programme("American Gangster", channel = "4Seven", runtime = 185).isLikelyFilm())
    }

    @Test
    fun `a film channel earns headroom a general channel does not`() {
        assertTrue(programme("Babylon", channel = "Film4", runtime = 220).isLikelyFilm())
        assertFalse(programme("Long Magazine Show", channel = "BBC Two", runtime = 220).isLikelyFilm())
    }

    @Test
    fun `a film channel rescues a borderline runtime`() {
        assertTrue(programme("Withnail and I", channel = "Film4", runtime = 75).isLikelyFilm())
        assertFalse(programme("Something", channel = "BBC Two", runtime = 75).isLikelyFilm())
    }

    @Test
    fun `filler is recognised whatever its shape`() {
        assertTrue(programme("Teleshopping").isScheduleFiller())
        assertTrue(programme("..programmes start at 6.00am").isScheduleFiller())
        assertTrue(programme("").isScheduleFiller())
        assertFalse(programme("Gogglebox").isScheduleFiller())
    }
}
