package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Loads listings from both EPG sources at once, publishing as they arrive.
 *
 * The two sources take very different times: the Freeview feed is one large
 * download, TVmaze is a request per day. Waiting for both before showing
 * anything meant the slower one set the time to first programme. Each result
 * is emitted as soon as it lands, so the guide appears on the first source and
 * refines when the second catches up.
 *
 * Either source failing is survivable — the other still produces a usable
 * guide. Only when both come back empty does it fall back to bundled sample
 * data, so the app never shows an empty wall.
 */
class EpgRepository(
    private val tvMaze: TvMazeApi = TvMazeApi(),
    private val freeview: FreeviewEpgApi = FreeviewEpgApi(),
) {

    data class Result(
        val programmes: List<Programme>,
        val isLive: Boolean,
        /** Which sources have answered so far. */
        val sources: Set<Source> = emptySet(),
        /** False while a source is still outstanding. */
        val isComplete: Boolean = true,
    )

    enum class Source { FREEVIEW_EPG, TVMAZE }

    fun load(days: Int = DEFAULT_DAYS, now: Long = System.currentTimeMillis()): Flow<Result> =
        channelFlow {
            val from = now - ONE_HOUR_MILLIS
            val to = now + TimeUnit.DAYS.toMillis(days.toLong())

            val lock = Mutex()
            var schedule = emptyList<Programme>()
            var enrichment = emptyList<Programme>()
            var outstanding = 2

            suspend fun publish() {
                val (result, finished) = lock.withLock {
                    outstanding -= 1
                    val sources = buildSet {
                        if (schedule.isNotEmpty()) add(Source.FREEVIEW_EPG)
                        if (enrichment.isNotEmpty()) add(Source.TVMAZE)
                    }
                    val merged = when {
                        sources.isNotEmpty() -> Result(
                            programmes = mergeProgrammes(schedule, enrichment),
                            isLive = true,
                            sources = sources,
                            isComplete = outstanding == 0,
                        )
                        outstanding == 0 -> Result(SampleData.programmes(now), isLive = false)
                        // Nothing yet and something still running: stay quiet.
                        else -> null
                    }
                    merged to (outstanding == 0)
                }
                result?.let { send(it) }
                if (finished) close()
            }

            launch {
                schedule = runCatching { freeview.listings(from, to) }.getOrDefault(emptyList())
                publish()
            }
            launch {
                enrichment = runCatching { tvMazeListings(days, from) }.getOrDefault(emptyList())
                publish()
            }

            awaitClose { }
        }

    /**
     * One request per day, run concurrently. Sequentially this was the slowest
     * part of a load by far — at a fortnight it is fourteen round trips, and
     * serialising them made the wait scale with the window the user asked for.
     */
    private suspend fun tvMazeListings(days: Int, from: Long): List<Programme> = coroutineScope {
        val today = LocalDate.now()
        (0 until days)
            .map { offset ->
                async {
                    runCatching { tvMaze.schedule(today.plusDays(offset.toLong())) }
                        .getOrDefault(emptyList())
                }
            }
            .awaitAll()
            .flatten()
            .distinctBy { it.id }
            .filter { it.startMillis > from }
    }

    companion object {
        /**
         * A fortnight. The XMLTV feed publishes a week, so the back half is
         * whatever TVmaze knows about — thinner, but it is where a series
         * worth planning around shows up.
         */
        const val DEFAULT_DAYS = 14
        private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
