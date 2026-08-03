package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme

/**
 * Whether a listing looks like a film.
 *
 * Neither source says so: the XMLTV feed carries no `<category>` at all, and
 * TVmaze's schedule covers series rather than films. So this is inference from
 * shape — a single long programme with no place in a series — and it is
 * deliberately cautious, because a film wrongly demoted to a series is merely
 * filtered out of a rail, while a shopping block promoted to a film is put in
 * front of the viewer.
 *
 * Known limits: a feature-length one-off documentary counts as a film, and a
 * film broadcast in two parts does not.
 */
fun Programme.isLikelyFilm(): Boolean {
    if (season != null || episode != null) return false
    if (isScheduleFiller()) return false
    val runtime = runtimeMinutes ?: return false
    // A film channel shows little else, so it both rescues a borderline
    // runtime and earns a longer ceiling — a three-hour slot there is an epic,
    // whereas anywhere else it is a morning magazine show.
    val onFilmChannel = isFilmChannel(channelName)
    val allowed = if (onFilmChannel) FILM_CHANNEL_MINUTES else FILM_MINUTES
    if (runtime !in allowed) return false
    return runtime >= FEATURE_MINUTES || onFilmChannel
}

/**
 * Whether a programme is a series (as opposed to a film or schedule filler).
 */
fun Programme.isSeries(): Boolean {
    if (isScheduleFiller() || isLikelyFilm()) return false
    return true
}

/**
 * Deduplicates a list of programmes by title, keeping only the occurrence
 * airing soonest (the one with the earliest [Programme.startMillis]).
 */
fun List<Programme>.deduplicateSoonest(): List<Programme> =
    sortedBy { it.startMillis }
        .distinctBy { it.title.trim().lowercase() }


/**
 * Continuity slates, shopping blocks and overnight filler. They occupy long
 * unnumbered slots, which is exactly the shape of a film, so they have to be
 * named to be excluded.
 */
internal fun Programme.isScheduleFiller(): Boolean {
    val name = title.trim().lowercase()
    if (name.isEmpty()) return true
    if (name.startsWith(".")) return true
    return FILLER_MARKERS.any { it in name }
}

private fun isFilmChannel(channelName: String): Boolean =
    normaliseChannelName(channelName) in FILM_CHANNELS

private val FILLER_MARKERS = listOf(
    "teleshopping", "shopping", "programmes start at", "programmes resume",
    "this is bbc", "close", "no programmes", "to be announced", "tbа",
)

private val FILM_CHANNELS = setOf("film4", "talking pictures tv", "great! movies", "legend")

/** Long enough to be a feature, short enough not to be a morning magazine show. */
private val FILM_MINUTES = 70..195

/** A film channel earns the headroom for an epic plus advertising breaks. */
private val FILM_CHANNEL_MINUTES = 70..240

private const val FEATURE_MINUTES = 80

/**
 * Whether this programme is on air right now.
 *
 * Where the runtime is unknown, a slot is assumed to run an hour — long enough
 * to cover most programmes and short enough that a stale listing does not offer
 * to tune to something that finished this morning.
 */
fun Programme.isOnNow(now: Long = System.currentTimeMillis()): Boolean {
    val length = (runtimeMinutes ?: ASSUMED_RUNTIME_MINUTES) * 60_000L
    return now >= startMillis && now < startMillis + length
}

private const val ASSUMED_RUNTIME_MINUTES = 60
