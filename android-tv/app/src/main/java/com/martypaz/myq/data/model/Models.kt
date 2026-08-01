package com.martypaz.myq.data.model

import kotlinx.serialization.Serializable

/** Where a channel is delivered. */
enum class Platform { FREEVIEW, STREAMING }

/** How "new" a programme is; drives badges and the new-programme rails. */
enum class Newness {
    NEW_SERIES,   // series 1, episode 1 — a brand new show
    NEW_SEASON,   // episode 1 of a returning series
    NEW_EPISODE,  // first broadcast of an episode
    NONE,
}

@Serializable
data class Programme(
    val id: String,
    val title: String,
    val episodeTitle: String? = null,
    val synopsis: String = "",
    val channelName: String,
    val platform: Platform,
    val genres: List<String> = emptyList(),
    /** Epoch milliseconds of the scheduled start. */
    val startMillis: Long,
    /** Runtime in minutes, when known. */
    val runtimeMinutes: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val newness: Newness = Newness.NONE,
    val imageUrl: String? = null,
)

/** A reminder the user has set against a programme. */
@Serializable
data class Reminder(
    val programmeId: String,
    val title: String,
    val channelName: String,
    val startMillis: Long,
    /** How many hours before the start the notification should fire. */
    val leadHours: Int,
) {
    val fireAtMillis: Long get() = startMillis - leadHours * 60L * 60L * 1000L
}

/** One horizontal rail on the home screen. */
data class Rail(
    val id: String,
    val title: String,
    val programmes: List<Programme>,
)
