package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme
import java.time.LocalDate

/**
 * Loads the next few days of listings, deduplicated and sorted by start time.
 * Falls back to bundled sample data when every network fetch fails, so the
 * app never shows an empty wall.
 */
class EpgRepository(private val api: TvMazeApi = TvMazeApi()) {

    data class Result(val programmes: List<Programme>, val isLive: Boolean)

    suspend fun load(days: Int = 3): Result {
        val today = LocalDate.now()
        val fetched = (0 until days)
            .flatMap { offset -> runCatching { api.schedule(today.plusDays(offset.toLong())) }.getOrDefault(emptyList()) }
            .distinctBy { it.id }
            .filter { it.startMillis > System.currentTimeMillis() - ONE_HOUR_MILLIS }
            .sortedBy { it.startMillis }

        return if (fetched.isNotEmpty()) {
            Result(fetched, isLive = true)
        } else {
            Result(SampleData.programmes(), isLive = false)
        }
    }

    private companion object {
        const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
