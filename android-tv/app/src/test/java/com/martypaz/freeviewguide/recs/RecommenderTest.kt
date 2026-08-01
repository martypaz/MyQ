package com.martypaz.freeviewguide.recs

import com.martypaz.freeviewguide.data.model.Newness
import com.martypaz.freeviewguide.data.model.Platform
import com.martypaz.freeviewguide.data.model.Programme
import com.martypaz.freeviewguide.data.prefs.TasteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommenderTest {

    private fun programme(
        id: String,
        genres: List<String> = emptyList(),
        channel: String = "BBC One",
        newness: Newness = Newness.NONE,
    ) = Programme(
        id = id, title = id, channelName = channel, platform = Platform.FREEVIEW,
        genres = genres, startMillis = 1_000L, newness = newness,
    )

    @Test
    fun `empty profile recommends nothing`() {
        val programmes = listOf(programme("a", genres = listOf("Drama")))
        assertEquals(emptyList<Programme>(), recommendForYou(TasteProfile(), programmes))
    }

    @Test
    fun `preferred genre ranks above unrelated programmes`() {
        val profile = TasteProfile(genreWeights = mapOf("Crime" to 5.0))
        val crime = programme("crime", genres = listOf("Crime"))
        val cooking = programme("cooking", genres = listOf("Food"))

        val result = recommendForYou(profile, listOf(cooking, crime))

        assertEquals(listOf(crime), result) // zero-scored programmes are excluded
    }

    @Test
    fun `channel affinity contributes at half weight`() {
        val profile = TasteProfile(channelWeights = mapOf("Dave" to 4.0))
        val onDave = programme("dave", channel = "Dave")
        assertEquals(2.0, scoreProgramme(profile, onDave), 1e-9)
    }

    @Test
    fun `new series outranks an equally matched repeat`() {
        val profile = TasteProfile(genreWeights = mapOf("Drama" to 2.0))
        val newSeries = programme("new", genres = listOf("Drama"), newness = Newness.NEW_SERIES)
        val repeat = programme("repeat", genres = listOf("Drama"))

        val result = recommendForYou(profile, listOf(repeat, newSeries))

        assertEquals(listOf("new", "repeat"), result.map { it.id })
    }

    @Test
    fun `limit caps the rail length`() {
        val profile = TasteProfile(genreWeights = mapOf("Drama" to 1.0))
        val many = (1..50).map { programme("p$it", genres = listOf("Drama")) }
        assertEquals(20, recommendForYou(profile, many).size)
    }

    @Test
    fun `weights decay about two percent per day`() {
        val dayMillis = 24L * 60L * 60L * 1000L
        val profile = TasteProfile(
            genreWeights = mapOf("Drama" to 10.0),
            lastDecayMillis = 1_000_000L,
        )
        val decayed = profile.decayed(now = 1_000_000L + dayMillis)
        assertEquals(9.8, decayed.genreWeights.getValue("Drama"), 1e-9)
    }

    @Test
    fun `near-zero weights are dropped by decay`() {
        val dayMillis = 24L * 60L * 60L * 1000L
        val profile = TasteProfile(
            genreWeights = mapOf("Drama" to 0.0101),
            lastDecayMillis = 0L,
        )
        // First touch just stamps the clock; a later decay pass prunes.
        val stamped = profile.decayed(now = 1_000_000L)
        val pruned = stamped.decayed(now = 1_000_000L + 100 * dayMillis)
        assertTrue(pruned.genreWeights.isEmpty())
    }
}
