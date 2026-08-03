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

/** The user's standing opinion of a series, set from the programme dialog. */
enum class Verdict { LOVE, HATE, NONE }

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
    /** How many minutes before the start the notification should fire. */
    val leadMinutes: Int = DEFAULT_LEAD_MINUTES,
    /**
     * Lead time as whole hours, which is how MyQ used to store it. Present so
     * reminders written by an older version still decode; [leadMinutes] is
     * what everything reads.
     */
    @Deprecated("Superseded by leadMinutes; retained so old reminders decode.")
    val leadHours: Int? = null,
) {
    /**
     * Reminders written before lead times went sub-hourly carry only
     * [leadHours], and a reminder that silently lost its lead time would fire
     * at the wrong moment rather than fail loudly.
     */
    @Suppress("DEPRECATION")
    val effectiveLeadMinutes: Int
        get() = when {
            leadMinutes != DEFAULT_LEAD_MINUTES -> leadMinutes
            leadHours != null -> leadHours * MINUTES_PER_HOUR
            else -> leadMinutes
        }

    val fireAtMillis: Long get() = startMillis - effectiveLeadMinutes * 60L * 1000L

    companion object {
        const val DEFAULT_LEAD_MINUTES = 60
        const val MINUTES_PER_HOUR = 60
    }
}

/** "5 minutes", "1 hour", "2 hours 30 minutes" — a lead time said aloud. */
fun formatLeadTime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val hourPart = when (hours) {
        0 -> null
        1 -> "1 hour"
        else -> "$hours hours"
    }
    val minutePart = when (rest) {
        0 -> null
        1 -> "1 minute"
        else -> "$rest minutes"
    }
    return listOfNotNull(hourPart, minutePart).joinToString(" ").ifEmpty { "0 minutes" }
}

/** Offered lead times: fine-grained where it matters, coarse where it does not. */
val LEAD_TIME_OPTIONS: List<Int> =
    listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720)

/**
 * A programme the user has marked to record.
 *
 * MyQ cannot drive the television's own tuner — Android reserves the DVR APIs
 * for system TV inputs — so this is MyQ's own record list: it tracks what you
 * want to catch and reminds you before it starts. [isSeries] marks the whole
 * series, so later episodes of the same title are picked up automatically.
 */
@Serializable
data class RecordEntry(
    val programmeId: String,
    val title: String,
    val channelName: String,
    val startMillis: Long,
    val isSeries: Boolean = false,
)

/** One horizontal rail on the home screen. */
data class Rail(
    val id: String,
    val title: String,
    val programmes: List<Programme>,
)
