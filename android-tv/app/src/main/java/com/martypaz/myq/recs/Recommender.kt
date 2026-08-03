package com.martypaz.myq.recs

import com.martypaz.myq.data.model.Newness
import com.martypaz.myq.data.model.Programme
import com.martypaz.myq.data.model.Verdict
import com.martypaz.myq.data.prefs.TasteProfile
import com.martypaz.myq.data.prefs.TasteStore
import kotlin.math.pow

/**
 * A small on-device recommender. Interactions bump genre/channel weights;
 * weights decay ~2% per day; programmes are scored against the profile to
 * build the "For You" rail. No data leaves the device.
 *
 * Scoring and decay are pure top-level functions so they stay unit-testable
 * off-device; this class only binds them to the persisted profile.
 */
class Recommender(private val store: TasteStore) {

    enum class Signal(val strength: Double) {
        BROWSED(0.3),       // dwelled on a card long enough to read the hero panel
        SELECTED(1.0),      // clicked through / opened a programme
        REMINDER_SET(3.0),  // asked to be reminded — the strongest intent we see
    }

    suspend fun record(programme: Programme, signal: Signal) {
        store.update { profile ->
            val decayed = profile.decayed()
            decayed.copy(
                genreWeights = programme.genres.fold(decayed.genreWeights) { acc, genre ->
                    acc + (genre to (acc[genre] ?: 0.0) + signal.strength)
                },
                channelWeights = decayed.channelWeights +
                    (programme.channelName to (decayed.channelWeights[programme.channelName] ?: 0.0) + signal.strength),
            )
        }
    }

    fun forYou(profile: TasteProfile, programmes: List<Programme>, limit: Int = 20): List<Programme> =
        recommendForYou(profile, programmes, limit)
}

/**
 * Highest-scoring upcoming programmes. Loved series always qualify; hated ones
 * never do. Otherwise the rail stays empty until the profile has some signal.
 */
fun recommendForYou(
    profile: TasteProfile,
    programmes: List<Programme>,
    limit: Int = 20,
): List<Programme> {
    val hasLearnedSignal = profile.genreWeights.isNotEmpty() || profile.channelWeights.isNotEmpty()
    val hasLoves = profile.verdicts.containsValue(Verdict.LOVE.name)
    if (!hasLearnedSignal && !hasLoves) return emptyList()

    return programmes
        .filterNot { profile.verdictFor(it.title) == Verdict.HATE }
        .map { it to scoreProgramme(profile, it) }
        .filter { (_, score) -> score > 0.0 }
        .sortedWith(compareByDescending<Pair<Programme, Double>> { it.second }.thenBy { it.first.startMillis })
        .distinctBy { it.first.title.trim().lowercase() }
        .take(limit)
        .map { (programme, _) -> programme }
}

internal fun scoreProgramme(profile: TasteProfile, programme: Programme): Double {
    return when (profile.verdictFor(programme.title)) {
        // An explicit opinion is the whole answer — no learned weight overrides it.
        Verdict.HATE -> 0.0
        Verdict.LOVE -> LOVE_SCORE + learnedScore(profile, programme)
        Verdict.NONE -> learnedScore(profile, programme)
    }
}

private fun learnedScore(profile: TasteProfile, programme: Programme): Double {
    val genreScore = programme.genres.sumOf { profile.genreWeights[it] ?: 0.0 }
    val channelScore = profile.channelWeights[programme.channelName] ?: 0.0
    val newnessBoost = when (programme.newness) {
        Newness.NEW_SERIES -> 1.5
        Newness.NEW_SEASON -> 1.2
        else -> 1.0
    }
    return (genreScore + 0.5 * channelScore) * newnessBoost
}

/** Large enough that any loved series outranks anything merely inferred. */
private const val LOVE_SCORE = 1_000.0

/** Applies ~2%/day decay so old obsessions fade; drops near-zero weights. */
internal fun TasteProfile.decayed(now: Long = System.currentTimeMillis()): TasteProfile {
    if (lastDecayMillis == 0L) return copy(lastDecayMillis = now)
    val days = ((now - lastDecayMillis) / MILLIS_PER_DAY).toInt()
    if (days <= 0) return this
    val factor = DAILY_DECAY.pow(days)
    return TasteProfile(
        genreWeights = genreWeights.mapValues { it.value * factor }.filterValues { it > 0.01 },
        channelWeights = channelWeights.mapValues { it.value * factor }.filterValues { it > 0.01 },
        lastDecayMillis = now,
    )
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
private const val DAILY_DECAY = 0.98
