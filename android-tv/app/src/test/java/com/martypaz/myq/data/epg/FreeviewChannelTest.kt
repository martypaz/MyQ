package com.martypaz.myq.data.epg

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeviewChannelTest {

    @Test
    fun `core channels match`() {
        listOf("BBC One", "ITV1", "Channel 4", "Channel 5", "Film4", "Dave").forEach {
            assertTrue("$it should be Freeview", isFreeviewChannel(it))
        }
    }

    @Test
    fun `matching is case-insensitive`() {
        assertTrue(isFreeviewChannel("bbc one"))
        assertTrue(isFreeviewChannel("FILM4"))
    }

    @Test
    fun `UKTV rebrand prefix is ignored`() {
        assertTrue(isFreeviewChannel("U&Dave"))
        assertTrue(isFreeviewChannel("U&Yesterday"))
        assertTrue(isFreeviewChannel("U&W"))
        assertTrue(isFreeviewChannel("U&Drama"))
    }

    /**
     * TVmaze names the main Channel 5 service "5", and the XMLTV feed calls it
     * "5 HD". Neither matched, so every Channel 5 programme was dropped from
     * the guide — on some days the largest single channel in the feed.
     */
    @Test
    fun `Channel 5 matches whatever a source calls it`() {
        listOf("5", "5 HD", "Channel 5", "channel 5").forEach {
            assertTrue("$it should be Freeview", isFreeviewChannel(it))
        }
    }

    @Test
    fun `HD and +1 variants match the base channel`() {
        listOf("BBC One HD", "ITV1 HD", "E4 +1", "Film4+1").forEach {
            assertTrue("$it should be Freeview", isFreeviewChannel(it))
        }
    }

    @Test
    fun `regional opt-outs match the national channel`() {
        listOf("BBC One London HD", "ITV1 Anglia HD", "Channel 4 Midlands HD").forEach {
            assertTrue("$it should be Freeview", isFreeviewChannel(it))
        }
    }

    @Test
    fun `pay TV and streaming channels do not match`() {
        listOf("Sky Atlantic", "Sky Max", "Netflix", "TNT Sports", "Gold").forEach {
            assertFalse("$it should not be Freeview", isFreeviewChannel(it))
        }
    }
}
