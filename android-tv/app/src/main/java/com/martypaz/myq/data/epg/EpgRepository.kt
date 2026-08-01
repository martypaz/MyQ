package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Loads the next few days of listings from both EPG sources at once.
 *
 * The Freeview XMLTV feed supplies the schedule — every channel, every slot —
 * and TVmaze supplies the metadata the feed lacks, chiefly genres, which is
 * what the recommender learns from. Either source failing is survivable: the
 * other still produces a usable guide. Only when both come back empty does it
 * fall back to bundled sample data, so the app never shows an empty wall.
 */
class EpgRepository(
    private val tvMaze: TvMazeApi = TvMazeApi(),
    private val freeview: FreeviewEpgApi = FreeviewEpgApi(),
) {

    data class Result(
        val programmes: List<Programme>,
        val isLive: Boolean,
        /** Which sources answered, for the offline banner and Settings. */
        val sources: Set<Source> = emptySet(),
    )

    enum class Source { FREEVIEW_EPG, TVMAZE }

    suspend fun load(days: Int = 3, now: Long = System.currentTimeMillis()): Result = coroutineScope {
        val from = now - ONE_HOUR_MILLIS
        val to = now + TimeUnit.DAYS.toMillis(days.toLong())

        // Concurrent: one is a 20MB download, the other is several small
        // requests, and there is no reason for either to wait on the other.
        val freeviewJob = async { runCatching { freeview.listings(from, to) }.getOrDefault(emptyList()) }
        val tvMazeJob = async { runCatching { tvMazeListings(days, from) }.getOrDefault(emptyList()) }

        val schedule = freeviewJob.await()
        val enrichment = tvMazeJob.await()

        val sources = buildSet {
            if (schedule.isNotEmpty()) add(Source.FREEVIEW_EPG)
            if (enrichment.isNotEmpty()) add(Source.TVMAZE)
        }

        if (sources.isEmpty()) {
            Result(SampleData.programmes(), isLive = false)
        } else {
            Result(mergeProgrammes(schedule, enrichment), isLive = true, sources = sources)
        }
    }

    private suspend fun tvMazeListings(days: Int, from: Long): List<Programme> {
        val today = LocalDate.now()
        return (0 until days)
            .flatMap { offset ->
                runCatching { tvMaze.schedule(today.plusDays(offset.toLong())) }.getOrDefault(emptyList())
            }
            .distinctBy { it.id }
            .filter { it.startMillis > from }
    }

    private companion object {
        const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
