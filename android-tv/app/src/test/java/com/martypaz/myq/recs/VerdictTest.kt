package com.martypaz.myq.recs

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.data.prefs.TasteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Love/Hate must outrank whatever the learned weights say. */
class VerdictTest {

    private fun programme(title: String, genres: List<String> = listOf("Reality")) = Programme(
        id = title, title = title, channelName = "ITV2", platform = Platform.FREEVIEW,
        genres = genres, startMillis = 1_000L, newness = Newness.NONE,
    )

    @Test
    fun `hated series never appears however strong the genre score`() {
        val profile = TasteProfile(
            genreWeights = mapOf("Reality" to 99.0),
            verdicts = mapOf("Love Island" to Verdict.HATE.name),
        )
        val result = recommendForYou(profile, listOf(programme("Love Island")))
        assertEquals(emptyList<Programme>(), result)
    }

    @Test
    fun `loved series outranks a high-scoring unrated one`() {
        val profile = TasteProfile(
            genreWeights = mapOf("Reality" to 50.0),
            verdicts = mapOf("Taskmaster" to Verdict.LOVE.name),
        )
        val result = recommendForYou(profile, listOf(programme("Big Brother"), programme("Taskmaster")))
        assertEquals("Taskmaster", result.first().title)
    }

    @Test
    fun `a love alone is enough to populate an otherwise empty profile`() {
        val profile = TasteProfile(verdicts = mapOf("Taskmaster" to Verdict.LOVE.name))
        val result = recommendForYou(profile, listOf(programme("Taskmaster"), programme("Other")))
        assertEquals(listOf("Taskmaster"), result.map { it.title })
    }

    @Test
    fun `no opinions and no learned weights still yields nothing`() {
        assertTrue(recommendForYou(TasteProfile(), listOf(programme("Anything"))).isEmpty())
    }

    @Test
    fun `hated series scores zero`() {
        val profile = TasteProfile(
            genreWeights = mapOf("Reality" to 10.0),
            verdicts = mapOf("Love Island" to Verdict.HATE.name),
        )
        assertEquals(0.0, scoreProgramme(profile, programme("Love Island")), 1e-9)
    }

    @Test
    fun `verdictFor tolerates unknown stored values`() {
        val profile = TasteProfile(verdicts = mapOf("Whatever" to "NOT_A_VERDICT"))
        assertEquals(Verdict.NONE, profile.verdictFor("Whatever"))
    }
}
