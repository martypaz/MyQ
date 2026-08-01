package com.martypaz.freeviewguide.recs

import com.martypaz.freeviewguide.data.model.Newness
import com.martypaz.freeviewguide.data.model.Programme
import com.martypaz.freeviewguide.data.prefs.TasteProfile
import com.martypaz.freeviewguide.data.prefs.TasteStore
import kotlin.math.pow

/**
 * A small on-device recommender. Interactions bump genre/channel weights;
 * weights decay ~2% per day; programmes are scored against the profile to
 * build the "For You" rail. No data leaves the device.
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

    /** Highest-scoring upcoming programmes; empty until the profile has any signal. */
    fun forYou(profile: TasteProfile, programmes: List<Programme>, limit: Int = 20): List<Programme> {
        if (profile.genreWeights.isEmpty() && profile.channelWeights.isEmpty()) return emptyList()
        return programmes
            .map { it to score(profile, it) }
            .filter { (_, score) -> score > 0.0 }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (programme, _) -> programme }
    }

    private fun score(profile: TasteProfile, programme: Programme): Double {
        val genreScore = programme.genres.sumOf { profile.genreWeights[it] ?: 0.0 }
        val channelScore = profile.channelWeights[programme.channelName] ?: 0.0
        val newnessBoost = when (programme.newness) {
            Newness.NEW_SERIES -> 1.5
            Newness.NEW_SEASON -> 1.2
            else -> 1.0
        }
        return (genreScore + 0.5 * channelScore) * newnessBoost
    }

    private fun TasteProfile.decayed(now: Long = System.currentTimeMillis()): TasteProfile {
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

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val DAILY_DECAY = 0.98
    }
}
