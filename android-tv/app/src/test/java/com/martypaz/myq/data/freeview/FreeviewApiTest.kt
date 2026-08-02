package com.martypaz.myq.data.freeview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FreeviewApiTest {

    @Test
    fun `accepts the offset form the guide actually publishes`() {
        // Freeview writes "+0000", not "+00:00", which OffsetDateTime.parse
        // rejects by default — every event was being dropped.
        val compact = parseFreeviewTime("2026-08-04T19:00:00+0000")
        val colon = parseFreeviewTime("2026-08-04T19:00:00+00:00")
        assertEquals(colon, compact)
        assertNotNull(compact)
    }

    @Test
    fun `honours the offset rather than assuming UTC`() {
        val utc = requireNotNull(parseFreeviewTime("2026-08-04T19:00:00+0000"))
        val british = requireNotNull(parseFreeviewTime("2026-08-04T19:00:00+0100"))
        assertEquals(60L * 60L * 1000L, utc - british)
    }

    @Test
    fun `rejects what it cannot read`() {
        assertNull(parseFreeviewTime(""))
        assertNull(parseFreeviewTime("   "))
        assertNull(parseFreeviewTime("not a time"))
    }

    @Test
    fun `parses ISO durations into minutes`() {
        assertEquals(180, parseIsoMinutes("PT3H"))
        assertEquals(60, parseIsoMinutes("PT1H"))
        assertEquals(45, parseIsoMinutes("PT45M"))
        assertEquals(5, parseIsoMinutes("PT5M"))
        assertEquals(90, parseIsoMinutes("PT1H30M"))
    }

    @Test
    fun `rejects durations that cannot be a programme`() {
        assertNull(parseIsoMinutes(null))
        assertNull(parseIsoMinutes(""))
        assertNull(parseIsoMinutes("3 hours"))
        assertNull(parseIsoMinutes("PT0S"))
        // A slot longer than ten hours is a fault in the feed, not a programme.
        assertNull(parseIsoMinutes("PT24H"))
    }
}
