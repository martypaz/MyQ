package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decodes captured-shape TVmaze payloads and maps them to programmes. */
class TvMazeParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val api = TvMazeApi()

    private val broadcastEpisode = """
        {
          "id": 3141592,
          "name": "Fire in the Hole",
          "season": 1,
          "number": 1,
          "airstamp": "2026-08-01T21:00:00+01:00",
          "runtime": 60,
          "summary": "<p>Raylan strikes a deal with Ava.</p>",
          "unknown_field": {"nested": true},
          "show": {
            "name": "Justified",
            "genres": ["Drama", "Crime"],
            "premiered": "2026-07-25",
            "summary": "<p>Action crime drama series.</p>",
            "averageRuntime": 60,
            "network": {"name": "Channel 4", "country": {"code": "GB"}},
            "webChannel": null,
            "image": {"medium": "https://example.test/m.jpg", "original": "https://example.test/o.jpg"}
          }
        }
    """.trimIndent()

    private val webEpisode = """
        {
          "id": 2718281,
          "name": "Signal",
          "season": 2,
          "number": 1,
          "airstamp": "2026-08-01T06:00:00+00:00",
          "runtime": null,
          "summary": null,
          "_embedded": {
            "show": {
              "name": "Orbital",
              "genres": ["Science-Fiction"],
              "premiered": "2025-05-01",
              "summary": "<p>A satellite engineer discovers a signal.</p>",
              "averageRuntime": 45,
              "network": null,
              "webChannel": {"name": "ITVX", "country": {"code": "GB"}}
            }
          }
        }
    """.trimIndent()

    @Test
    fun `broadcast episode decodes and maps`() {
        val episode = json.decodeFromString<TvMazeEpisode>(broadcastEpisode)
        val programme = with(api) { episode.toProgramme(Platform.FREEVIEW) }!!

        assertEquals("Justified", programme.title)
        assertEquals("Fire in the Hole", programme.episodeTitle)
        assertEquals("Channel 4", programme.channelName)
        assertEquals("Raylan strikes a deal with Ava.", programme.synopsis)
        assertEquals(Newness.NEW_SERIES, programme.newness)
        assertEquals(60, programme.runtimeMinutes)
        assertEquals("https://example.test/o.jpg", programme.imageUrl)
        assertTrue(programme.startMillis > 0)
    }

    @Test
    fun `web episode with embedded show decodes and maps`() {
        val episode = json.decodeFromString<TvMazeEpisode>(webEpisode)
        val programme = with(api) { episode.toProgramme(Platform.STREAMING) }!!

        assertEquals("Orbital", programme.title)
        assertEquals("ITVX", programme.channelName)
        assertEquals(Newness.NEW_SEASON, programme.newness)
        // Falls back to the show synopsis, stripped of markup.
        assertEquals("A satellite engineer discovers a signal.", programme.synopsis)
        // Falls back to averageRuntime when episode runtime is null.
        assertEquals(45, programme.runtimeMinutes)
    }

    @Test
    fun `episode without a show is dropped`() {
        val episode = json.decodeFromString<TvMazeEpisode>("""{"id": 1, "airstamp": "2026-08-01T21:00:00+01:00"}""")
        assertNull(with(api) { episode.toProgramme(Platform.FREEVIEW) })
    }

    @Test
    fun `episode without an airstamp is dropped`() {
        val episode = json.decodeFromString<TvMazeEpisode>(
            """{"id": 2, "show": {"name": "X", "network": {"name": "BBC One"}}}""",
        )
        assertNull(with(api) { episode.toProgramme(Platform.FREEVIEW) })
    }

    @Test
    fun `schedule array decodes`() {
        val episodes = json.decodeFromString<List<TvMazeEpisode>>("[$broadcastEpisode, $webEpisode]")
        assertEquals(2, episodes.size)
    }
}
