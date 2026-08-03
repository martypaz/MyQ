package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Programme

/**
 * Folds the two EPG sources into one listing.
 *
 * They are complementary rather than redundant. The XMLTV feed has the whole
 * Freeview line-up slot by slot but no genres; TVmaze has genres, premiere
 * dates and show artwork for the far smaller set of programmes it tracks. A
 * viewer should see the first, and the recommender needs the second.
 *
 * So: take every programme from both, and where they describe the same
 * broadcast, keep one record carrying the best fields of each.
 *
 * @param schedule the broad listing, which decides what exists.
 * @param enrichment the richer metadata, which fills in the gaps.
 */
fun mergeProgrammes(
    schedule: List<Programme>,
    enrichment: List<Programme>,
): List<Programme> {
    val byKey = LinkedHashMap<String, Programme>(schedule.size + enrichment.size)

    schedule.forEach { programme ->
        byKey.merge(broadcastKey(programme), programme) { existing, _ -> existing }
    }
    enrichment.forEach { extra ->
        val key = broadcastKey(extra)
        val existing = byKey[key]
        byKey[key] = existing?.enrichedWith(extra) ?: extra
    }

    return byKey.values.sortedBy { it.startMillis }
}

/**
 * Identifies a broadcast independently of which source described it: the same
 * title, on the same channel, at the same time.
 *
 * Start times are bucketed to five minutes because sources round differently —
 * one listing may say 21:00 where the other says 20:59 — and a channel cannot
 * show two different programmes inside one bucket anyway.
 *
 * This is also how anything the user saved against a programme finds that
 * programme again after its id changes; see the reminder migration.
 */
internal fun broadcastKey(channelName: String, title: String, startMillis: Long): String {
    val slot = startMillis / SLOT_MILLIS
    return "${normaliseChannelName(channelName)}|$slot|${title.trim().lowercase()}"
}

internal fun broadcastKey(programme: Programme): String =
    broadcastKey(programme.channelName, programme.title, programme.startMillis)

/**
 * Fills this programme's blanks from [other] without ever overwriting what it
 * already knows. Field-wise rather than record-wise, because neither source is
 * uniformly better: the schedule owns times and channel, the enrichment owns
 * genres and premiere-derived newness.
 */
internal fun Programme.enrichedWith(other: Programme): Programme = copy(
    episodeTitle = episodeTitle ?: other.episodeTitle,
    synopsis = synopsis.ifBlank { other.synopsis },
    genres = genres.ifEmpty { other.genres },
    runtimeMinutes = runtimeMinutes ?: other.runtimeMinutes,
    season = season ?: other.season,
    episode = episode ?: other.episode,
    // NONE means "not known", which every XMLTV record is.
    newness = if (newness == Newness.NONE) other.newness else newness,
    imageUrl = imageUrl ?: other.imageUrl,
)

private const val SLOT_MILLIS = 5L * 60L * 1000L
