package com.martypaz.myq.data.freeview

import com.martypaz.myq.data.epg.EpgRepository
import com.martypaz.myq.data.epg.EpgSource
import com.martypaz.myq.data.epg.displayChannelName
import com.martypaz.myq.data.epg.isFreeviewChannel
import com.martypaz.myq.data.epg.isTimeshiftChannel
import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Platform
import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Listings from Freeview's own guide.
 *
 * The best-informed of the sources for UK broadcast television: it is keyed on
 * a network id that identifies the transmitter region, so the line-up and the
 * channel names are the ones the viewer's aerial actually receives — "BBC ONE
 * Wales", not "BBC One". A single day carries roughly 150 channels and several
 * thousand programmes.
 *
 * Two things it publishes are deliberately not used. Genres arrive as opaque
 * URNs ("urn:fvc:metadata:cs:ContentSubjectCS:2014-07:2") with no codelist to
 * resolve them, and its on-demand links are HbbTV broadcast-app launch URLs
 * for television platform players rather than anything an Android app can
 * open. Guessing at either would put wrong information in front of the viewer;
 * TVmaze continues to supply genres.
 *
 * The `Authorization` header is the fixed credential Freeview's own web guide
 * ships to every browser, not a personal one.
 */
class FreeviewApi(
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = BASE_URL,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** The transmitter region for a postcode, or null if it cannot be resolved. */
    suspend fun networkFor(postcode: String): FreeviewNetwork? = withContext(Dispatchers.IO) {
        val cleaned = postcode.trim()
        if (cleaned.isEmpty()) return@withContext null
        val encoded = java.net.URLEncoder.encode(cleaned, "UTF-8")
        runCatching {
            val body = get("$baseUrl/get-network-id?postcode=$encoded")
            json.decodeFromString<NetworkResponse>(body).data
        }.getOrNull()
    }

    /** One day of listings for [networkId], starting at [startEpochSeconds]. */
    suspend fun guide(networkId: Int, startEpochSeconds: Long): List<Programme> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = get("$baseUrl/tv-guide?nid=$networkId&start=$startEpochSeconds")
                json.decodeFromString<GuideResponse>(body).data.programs.flatMap { it.toProgrammes() }
            }.getOrDefault(emptyList())
        }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", WEB_GUIDE_CREDENTIAL)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Freeview responded ${response.code} for $url")
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        const val BASE_URL = "https://www.freeview.co.uk/api"

        /** Shipped in Freeview's own web guide; shared, not personal. */
        private const val WEB_GUIDE_CREDENTIAL = "Basic ZXR2ZGV2ZWxvcG1lbnQ6M3R2ZDN2YWNjMzU1"
    }
}

@Serializable
data class FreeviewNetwork(val network_id: Int, val network_name: String = "")

@Serializable
private data class NetworkResponse(val data: FreeviewNetwork)

@Serializable
private data class GuideResponse(val data: GuideData)

@Serializable
private data class GuideData(val programs: List<GuideChannel> = emptyList())

@Serializable
private data class GuideChannel(
    val service_id: String = "",
    val title: String = "",
    val events: List<GuideEvent> = emptyList(),
) {
    fun toProgrammes(): List<Programme> {
        if (title.isBlank() || !isFreeviewChannel(title) || isTimeshiftChannel(title)) return emptyList()
        val channel = displayChannelName(title)
        return events.mapNotNull { it.toProgramme(service_id, channel) }
    }
}

@Serializable
private data class GuideEvent(
    val program_id: String = "",
    val main_title: String = "",
    val secondary_title: String? = null,
    val image_url: String? = null,
    val fallback_image_url: String? = null,
    val start_time: String = "",
    val duration: String? = null,
) {
    fun toProgramme(serviceId: String, channelName: String): Programme? {
        if (main_title.isBlank()) return null
        val start = parseFreeviewTime(start_time) ?: return null
        return Programme(
            // The slot identifies the broadcast; program_id is a content id
            // that repeats across showings.
            id = "freeview-$serviceId-$start",
            title = main_title.trim(),
            episodeTitle = secondary_title?.trim()?.takeIf { it.isNotEmpty() && !it.isDateOnly() },
            channelName = channelName,
            platform = Platform.FREEVIEW,
            startMillis = start,
            runtimeMinutes = parseIsoMinutes(duration),
            // Freeview marks no premieres, and inferring one would badge reruns.
            newness = Newness.NONE,
            imageUrl = image_url?.takeIf { it.isNotBlank() } ?: fallback_image_url?.takeIf { it.isNotBlank() },
        )
    }
}

/** Secondary titles are sometimes just the broadcast date, which is not an episode. */
private fun String.isDateOnly(): Boolean = matches(Regex("""\d{2}/\d{2}/\d{4}"""))

/**
 * Freeview stamps offsets without a colon ("+0000"), which is valid ISO-8601
 * but not the subset [OffsetDateTime.parse] accepts by default. Both forms are
 * tried, because a source whose every timestamp fails contributes nothing and
 * says nothing about why.
 */
internal fun parseFreeviewTime(raw: String): Long? {
    val value = raw.trim().takeIf { it.isNotEmpty() } ?: return null
    return runCatching { OffsetDateTime.parse(value) }
        .recoverCatching { OffsetDateTime.parse(value, COMPACT_OFFSET) }
        .getOrNull()
        ?.toInstant()
        ?.toEpochMilli()
}

private val COMPACT_OFFSET: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

/** Durations are ISO-8601 ("PT3H", "PT45M"). */
internal fun parseIsoMinutes(raw: String?): Int? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { Duration.parse(value).toMinutes().toInt() }
        .getOrNull()
        ?.takeIf { it in 1..600 }
}

/**
 * [FreeviewApi] as a uniform listings source.
 *
 * The guide is served a day at a time, so a window becomes one request per day
 * and those run concurrently. Without a network id — no postcode set, or a
 * lookup that failed — this contributes nothing and the other sources build
 * the guide.
 */
class FreeviewSource(
    private val api: FreeviewApi,
    private val networkId: () -> Int?,
) : EpgSource {

    override val source = EpgRepository.Source.FREEVIEW_UK

    override suspend fun listings(fromMillis: Long, toMillis: Long): List<Programme> = coroutineScope {
        val nid = networkId() ?: return@coroutineScope emptyList()
        val firstDay = (fromMillis / MILLIS_PER_DAY) * MILLIS_PER_DAY / 1000L
        val days = ((toMillis - fromMillis) / MILLIS_PER_DAY).toInt().coerceIn(1, MAX_DAYS)

        (0 until days)
            .map { offset -> async { api.guide(nid, firstDay + offset * SECONDS_PER_DAY) } }
            .awaitAll()
            .flatten()
            .distinctBy { it.id }
            .filter { it.startMillis in fromMillis..toMillis }
            .sortedBy { it.startMillis }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val SECONDS_PER_DAY = 24L * 60L * 60L
        /** The guide does not usefully reach beyond a week or so. */
        const val MAX_DAYS = 8
    }
}
