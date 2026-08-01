package com.martypaz.myq.data.tv

import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import org.junit.Assert.assertEquals
import org.junit.Test

class TvLineupTest {

    private fun programme(title: String, channel: String) = Programme(
        id = title, title = title, channelName = channel,
        platform = Platform.FREEVIEW, startMillis = 1_000L,
    )

    private fun channel(name: String, number: String? = null) =
        TvChannel(id = name.hashCode().toLong(), displayName = name, number = number)

    @Test
    fun `keeps only what the television receives`() {
        val kept = restrictToLineup(
            programmes = listOf(
                programme("Gogglebox", "Channel 4"),
                programme("Pay TV Thing", "Sky Atlantic"),
            ),
            lineup = listOf(channel("Channel 4")),
        )
        assertEquals(listOf("Gogglebox"), kept.map { it.title })
    }

    @Test
    fun `matches across the spellings a tuner and a feed use`() {
        val kept = restrictToLineup(
            listOf(programme("24 Hours in A&E", "5")),
            listOf(channel("5 HD", number = "5")),
        )
        assertEquals(1, kept.size)
    }

    @Test
    fun `an unknown line-up leaves the guide alone`() {
        val programmes = listOf(programme("Gogglebox", "Channel 4"))
        assertEquals(programmes, restrictToLineup(programmes, emptyList()))
    }

    @Test
    fun `a line-up that matches nothing leaves the guide alone`() {
        // Names disagreeing is far likelier than a television receiving
        // nothing, and an empty guide is the worse failure.
        val programmes = listOf(programme("Gogglebox", "Channel 4"))
        assertEquals(programmes, restrictToLineup(programmes, listOf(channel("Nonsense Channel"))))
    }

    @Test
    fun `decoding absent genres yields nothing rather than failing`() {
        assertEquals(emptyList<String>(), decodeGenres(null))
        assertEquals(emptyList<String>(), decodeGenres(""))
        assertEquals(emptyList<String>(), decodeGenres("   "))
    }
}
