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

    @Test
    fun `pay TV and streaming channels do not match`() {
        listOf("Sky Atlantic", "Sky Max", "Netflix", "TNT Sports", "Gold").forEach {
            assertFalse("$it should not be Freeview", isFreeviewChannel(it))
        }
    }
}
