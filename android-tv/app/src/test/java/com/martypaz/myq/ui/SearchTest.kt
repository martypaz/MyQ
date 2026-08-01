package com.martypaz.myq.ui

import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.ui.screens.searchProgrammes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    private val listings = listOf(
        programme("Detectorists", channel = "BBC Two", genres = listOf("Comedy")),
        programme("Midnight Signals", channel = "Channel 4", genres = listOf("Crime", "Drama")),
        programme("Orbital", channel = "ITVX", platform = Platform.STREAMING, genres = listOf("Science-Fiction")),
    )

    private fun programme(
        title: String,
        channel: String,
        platform: Platform = Platform.FREEVIEW,
        genres: List<String> = emptyList(),
    ) = Programme(
        id = title, title = title, channelName = channel, platform = platform,
        genres = genres, startMillis = 1_000L,
    )

    @Test
    fun `matches on title, case-insensitively`() {
        assertEquals(listOf("Detectorists"), searchProgrammes(listings, "detect").map { it.title })
    }

    @Test
    fun `matches on channel`() {
        assertEquals(listOf("Midnight Signals"), searchProgrammes(listings, "Channel 4").map { it.title })
    }

    @Test
    fun `matches on genre`() {
        assertEquals(listOf("Midnight Signals"), searchProgrammes(listings, "crime").map { it.title })
    }

    @Test
    fun `spans freeview and streaming`() {
        assertEquals(listOf("Orbital"), searchProgrammes(listings, "itvx").map { it.title })
    }

    @Test
    fun `blank query returns nothing rather than everything`() {
        assertTrue(searchProgrammes(listings, "   ").isEmpty())
    }
}
