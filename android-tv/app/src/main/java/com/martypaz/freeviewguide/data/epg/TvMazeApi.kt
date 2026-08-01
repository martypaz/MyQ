package com.martypaz.freeviewguide.data.epg

import com.martypaz.freeviewguide.data.model.Newness
import com.martypaz.freeviewguide.data.model.Platform
import com.martypaz.freeviewguide.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * EPG source backed by the free, keyless TVmaze schedule API.
 *
 * - `GET /schedule?country=GB&date=...`   → broadcast channels (Freeview and others)
 * - `GET /schedule/web?country=GB&date=…` → streaming/web channels (iPlayer, ITVX, Netflix, …)
 *
 * Broadcast results are filtered to a Freeview channel allowlist so pay-TV-only
 * channels don't leak into a Freeview guide.
 */
class TvMazeApi(private val client: OkHttpClient = OkHttpClient()) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun schedule(date: LocalDate): List<Programme> = withContext(Dispatchers.IO) {
        val broadcast = runCatching { fetch("https://api.tvmaze.com/schedule?country=GB&date=$date") }
            .getOrDefault(emptyList())
        val web = runCatching { fetch("https://api.tvmaze.com/schedule/web?country=GB&date=$date") }
            .getOrDefault(emptyList())

        val freeview = broadcast.mapNotNull { it.toProgramme(Platform.FREEVIEW) }
            .filter { it.channelName in FREEVIEW_CHANNELS }
        val streaming = web.mapNotNull { it.toProgramme(Platform.STREAMING) }
        freeview + streaming
    }

    private fun fetch(url: String): List<TvMazeEpisode> {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("TVmaze responded ${response.code} for $url")
            val body = response.body?.string() ?: return emptyList()
            return json.decodeFromString(body)
        }
    }

    private fun TvMazeEpisode.toProgramme(platform: Platform): Programme? {
        val show = show ?: embedded?.show ?: return null
        val startMillis = airstamp?.let {
            runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        } ?: return null
        val channel = show.network?.name ?: show.webChannel?.name ?: return null

        val premieredRecently = show.premiered?.let { premiered ->
            runCatching {
                ChronoUnit.DAYS.between(LocalDate.parse(premiered), LocalDate.now()) in 0..45
            }.getOrDefault(false)
        } ?: false

        val newness = when {
            season == 1 && number == 1 -> Newness.NEW_SERIES
            premieredRecently && (season ?: 1) == 1 -> Newness.NEW_SERIES
            number == 1 -> Newness.NEW_SEASON
            else -> Newness.NEW_EPISODE
        }

        return Programme(
            id = "tvmaze-$id",
            title = show.name ?: return null,
            episodeTitle = name,
            synopsis = (summary ?: show.summary).orEmpty().stripHtml(),
            channelName = channel,
            platform = platform,
            genres = show.genres,
            startMillis = startMillis,
            runtimeMinutes = runtime ?: show.averageRuntime,
            season = season,
            episode = number,
            newness = newness,
            imageUrl = image?.original ?: image?.medium
                ?: show.image?.original ?: show.image?.medium,
        )
    }

    private fun String.stripHtml(): String = replace(Regex("<[^>]*>"), "").trim()

    companion object {
        /** Names as TVmaze reports them for the main UK Freeview line-up. */
        val FREEVIEW_CHANNELS: Set<String> = setOf(
            "BBC One", "BBC Two", "BBC Three", "BBC Four", "CBBC", "CBeebies",
            "BBC News", "ITV", "ITV1", "ITV2", "ITV3", "ITV4", "ITVBe",
            "Channel 4", "E4", "More4", "Film4", "4seven",
            "Channel 5", "5USA", "5STAR", "5SELECT", "5ACTION",
            "Dave", "Drama", "Yesterday", "W", "Quest", "Quest Red",
            "Really", "DMAX", "Food Network", "HGTV", "Blaze",
            "Sky Arts", "Sky Mix", "Challenge", "Pick", "GREAT! TV",
            "Legend", "That's TV", "Together TV", "PBS America", "Talking Pictures TV",
            "London Live", "GB News", "TalkTV", "S4C", "STV", "U&Dave",
            "U&Drama", "U&Yesterday", "U&W",
        )
    }
}

// --- TVmaze wire types (subset) ---

@Serializable
data class TvMazeEpisode(
    val id: Long,
    val name: String? = null,
    val season: Int? = null,
    val number: Int? = null,
    val airstamp: String? = null,
    val runtime: Int? = null,
    val summary: String? = null,
    val image: TvMazeImage? = null,
    val show: TvMazeShow? = null,
    @SerialName("_embedded") val embedded: TvMazeEmbedded? = null,
)

@Serializable
data class TvMazeEmbedded(val show: TvMazeShow? = null)

@Serializable
data class TvMazeShow(
    val name: String? = null,
    val genres: List<String> = emptyList(),
    val premiered: String? = null,
    val summary: String? = null,
    val averageRuntime: Int? = null,
    val network: TvMazeChannel? = null,
    val webChannel: TvMazeChannel? = null,
    val image: TvMazeImage? = null,
)

@Serializable
data class TvMazeChannel(val name: String? = null)

@Serializable
data class TvMazeImage(val medium: String? = null, val original: String? = null)
