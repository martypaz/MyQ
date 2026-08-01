package com.martypaz.freeviewguide.data.epg

import com.martypaz.freeviewguide.data.model.Newness
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class NewnessTest {

    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun `series one episode one is a new series`() {
        assertEquals(Newness.NEW_SERIES, deriveNewness(season = 1, number = 1, premiered = null, today = today))
    }

    @Test
    fun `recently premiered first season counts as new series even mid-run`() {
        assertEquals(
            Newness.NEW_SERIES,
            deriveNewness(season = 1, number = 4, premiered = "2026-07-20", today = today),
        )
    }

    @Test
    fun `old first season mid-run is just a new episode`() {
        assertEquals(
            Newness.NEW_EPISODE,
            deriveNewness(season = 1, number = 4, premiered = "2020-01-01", today = today),
        )
    }

    @Test
    fun `episode one of a later season is a new season`() {
        assertEquals(Newness.NEW_SEASON, deriveNewness(season = 6, number = 1, premiered = "2015-01-01", today = today))
    }

    @Test
    fun `mid-season episode of a returning show is a new episode`() {
        assertEquals(Newness.NEW_EPISODE, deriveNewness(season = 6, number = 3, premiered = "2015-01-01", today = today))
    }

    @Test
    fun `future premiere date does not count as recent`() {
        assertEquals(
            Newness.NEW_EPISODE,
            deriveNewness(season = 1, number = 2, premiered = "2026-09-01", today = today),
        )
    }

    @Test
    fun `unparseable premiere date is tolerated`() {
        assertEquals(
            Newness.NEW_EPISODE,
            deriveNewness(season = 2, number = 5, premiered = "not-a-date", today = today),
        )
    }
}
