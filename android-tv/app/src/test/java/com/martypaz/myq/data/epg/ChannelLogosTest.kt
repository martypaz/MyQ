package com.martypaz.myq.data.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelLogosTest {

    @Test
    fun `finds a logo for the main freeview channels`() {
        assertNotNull(channelLogoUrl("BBC One"))
        assertNotNull(channelLogoUrl("ITV1"))
        assertNotNull(channelLogoUrl("Channel 4"))
        assertNotNull(channelLogoUrl("Dave"))
    }

    @Test
    fun `covers the plain 5 that TVmaze reports for Channel 5`() {
        val five = channelLogoUrl("5")
        assertNotNull(five)
        assertEquals(five, channelLogoUrl("Channel 5"))
    }

    @Test
    fun `folds away HD and +1 variants`() {
        val bbcOne = channelLogoUrl("BBC One")
        assertEquals(bbcOne, channelLogoUrl("BBC One HD"))
        assertEquals(channelLogoUrl("E4"), channelLogoUrl("E4 +1"))
        assertEquals(channelLogoUrl("E4"), channelLogoUrl("E4+1"))
    }

    @Test
    fun `folds away the UKTV rebrand prefix`() {
        assertEquals(channelLogoUrl("Dave"), channelLogoUrl("U&Dave"))
        assertEquals(channelLogoUrl("Yesterday"), channelLogoUrl("U&Yesterday"))
    }

    @Test
    fun `regional opt-outs share the national ident`() {
        val itv1 = channelLogoUrl("ITV1")
        assertEquals(itv1, channelLogoUrl("ITV1 London HD"))
        assertEquals(itv1, channelLogoUrl("ITV1 Anglia HD"))
        assertEquals(channelLogoUrl("BBC One"), channelLogoUrl("BBC One CI HD"))
        assertEquals(channelLogoUrl("Channel 4"), channelLogoUrl("Channel 4 Midlands HD"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(channelLogoUrl("BBC Two"), channelLogoUrl("bbc two"))
    }

    @Test
    fun `returns null for channels with no logo, so the card falls back to text`() {
        assertNull(channelLogoUrl("Some Channel That Does Not Exist"))
        assertNull(channelLogoUrl(""))
        assertNull(channelLogoUrl("BBC iPlayer"))
    }

    @Test
    fun `every freeview allowlist channel has an ident`() {
        val missing = FREEVIEW_CHANNELS.filter { channelLogoUrl(it) == null }
        assertEquals("channels with no logo: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `every url points at one of the two logo repos over https`() {
        val urls = FREEVIEW_CHANNELS.mapNotNull(::channelLogoUrl).distinct()
        assertTrue(urls.size > 30)
        urls.forEach { url ->
            assertTrue(url, url.startsWith("https://raw.githubusercontent.com/"))
            assertTrue(url, url.endsWith(".png"))
        }
    }
}
