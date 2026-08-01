package com.martypaz.myq.data.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingAppsTest {

    @Test
    fun `maps a broadcast channel to its catch-up service`() {
        assertEquals("BBC iPlayer", streamingAppFor("BBC One")?.displayName)
        assertEquals("Channel 4", streamingAppFor("E4")?.displayName)
        assertEquals("My5", streamingAppFor("5USA")?.displayName)
    }

    @Test
    fun `maps a streaming channel to its own app`() {
        assertEquals("Netflix", streamingAppFor("Netflix")?.displayName)
        assertEquals("ITVX", streamingAppFor("ITVX")?.displayName)
    }

    @Test
    fun `survives the UKTV rebrand prefix`() {
        assertEquals("U", streamingAppFor("Dave")?.displayName)
        assertEquals("U", streamingAppFor("U&Dave")?.displayName)
    }

    @Test
    fun `ignores HD and +1 variants of the same service`() {
        assertEquals("ITVX", streamingAppFor("ITV1 HD")?.displayName)
        assertEquals("Channel 4", streamingAppFor("E4 +1")?.displayName)
    }

    @Test
    fun `matches case-insensitively`() {
        assertEquals("BBC iPlayer", streamingAppFor("bbc two")?.displayName)
    }

    @Test
    fun `falls back to a family prefix for unlisted channels`() {
        assertEquals("BBC iPlayer", streamingAppFor("BBC Two Wales")?.displayName)
        assertEquals("ITVX", streamingAppFor("ITV9")?.displayName)
    }

    @Test
    fun `returns null when no app carries the channel, so the action can be hidden`() {
        assertNull(streamingAppFor("PBS America"))
        assertNull(streamingAppFor("Talking Pictures TV"))
        assertNull(streamingAppFor(""))
        assertNull(streamingAppFor("   "))
    }

    @Test
    fun `search url escapes the title`() {
        val iplayer = requireNotNull(streamingAppFor("BBC One"))
        assertEquals(
            "https://www.bbc.co.uk/iplayer/search?q=Bodies+%26+Bones",
            iplayer.searchUrl("  Bodies & Bones  "),
        )
    }

    @Test
    fun `store package is the first candidate, which is the android tv build`() {
        assertEquals("com.netflix.ninja", requireNotNull(streamingAppFor("Netflix")).storePackage)
    }

    @Test
    fun `every service can be both launched and searched`() {
        val services = listOf(
            "BBC One", "ITV1", "Channel 4", "Channel 5", "Dave", "STV",
            "Netflix", "Prime Video", "Disney+", "Apple TV+", "Paramount+",
            "Quest", "YouTube",
        ).mapNotNull(::streamingAppFor).distinctBy { it.id }

        assertEquals(13, services.size)
        services.forEach { service ->
            assertTrue(service.id, service.packages.isNotEmpty())
            assertTrue(service.id, service.packages.none { it.isBlank() })
            assertTrue(service.id, service.searchUrlTemplate.startsWith("https://"))
            assertTrue(service.id, service.displayName.isNotBlank())
        }
    }
}
