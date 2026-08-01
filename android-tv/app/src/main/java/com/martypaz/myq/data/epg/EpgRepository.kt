package com.martypaz.myq.data.epg

import com.martypaz.myq.data.model.Programme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Builds the guide from every listings source at once, publishing as they
 * arrive.
 *
 * The sources are interchangeable behind [EpgSource]; this class knows only
 * how to run them and in what order to fold their answers. Adding a fourth is
 * a constructor argument.
 *
 * Ordering is the design. [Source] is declared worst-to-best-informed about
 * *what exists*, and the merge folds in that order: the broadest schedule
 * decides which programmes there are and owns their identity, while narrower
 * sources fill in what they know better — genres, or what this particular
 * television receives — without being able to remove anything.
 *
 * They also take very different times. A 20MB download, a request per day, and
 * a local content provider should not be made to wait on each other, so each
 * result is emitted as it lands and the guide refines in place. Any source
 * failing is survivable; only when all of them come back empty does it fall
 * back to bundled sample data, so the app never shows an empty wall.
 */
class EpgRepository(private val sources: List<EpgSource>) {

    data class Result(
        val programmes: List<Programme>,
        val isLive: Boolean,
        /** Which sources have answered so far. */
        val sources: Set<Source> = emptySet(),
        /** False while a source is still outstanding. */
        val isComplete: Boolean = true,
    )

    /** Declared in merge order: earlier owns identity, later enriches. */
    enum class Source { FREEVIEW_EPG, DEVICE_TUNER, TVMAZE }

    fun load(days: Int = DEFAULT_DAYS, now: Long = System.currentTimeMillis()): Flow<Result> =
        channelFlow {
            val from = now - ONE_HOUR_MILLIS
            val to = now + TimeUnit.DAYS.toMillis(days.toLong())

            val lock = Mutex()
            val answers = LinkedHashMap<Source, List<Programme>>()
            var outstanding = sources.size

            if (outstanding == 0) {
                send(Result(SampleData.programmes(now), isLive = false))
                return@channelFlow
            }

            sources.forEach { source ->
                launch {
                    val listings = runCatching { source.listings(from, to) }.getOrDefault(emptyList())
                    val result = lock.withLock {
                        answers[source.source] = listings
                        outstanding -= 1
                        buildResult(answers, outstanding)
                    }
                    result?.let { send(it) }
                    if (result != null && result.isComplete) close()
                }
            }

            awaitClose { }
        }

    private fun buildResult(answers: Map<Source, List<Programme>>, outstanding: Int): Result? {
        val answered = answers.filterValues { it.isNotEmpty() }
        return when {
            answered.isNotEmpty() -> Result(
                programmes = mergeAll(Source.entries.mapNotNull { answered[it] }),
                isLive = true,
                sources = answered.keys,
                isComplete = outstanding == 0,
            )
            outstanding == 0 -> Result(SampleData.programmes(), isLive = false)
            // Nothing yet and something still running: stay quiet.
            else -> null
        }
    }

    companion object {
        /**
         * A fortnight. The XMLTV feed publishes a week and the tuner usually
         * less, so the back half is whatever TVmaze knows about — thinner, but
         * it is where a series worth planning around shows up.
         */
        const val DEFAULT_DAYS = 14
        private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
    }
}
