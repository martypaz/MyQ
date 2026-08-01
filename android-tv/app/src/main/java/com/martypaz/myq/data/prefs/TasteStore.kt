package com.martypaz.myq.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.martypaz.myq.data.model.Verdict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.tasteDataStore by preferencesDataStore(name = "taste")

/**
 * The learned taste profile: per-genre and per-channel weights plus a decay
 * timestamp. Weights rise when the user interacts with a programme and decay
 * a little every day so old obsessions fade.
 *
 * [verdicts] holds explicit per-series opinions (title -> LOVE/HATE) set from
 * the programme dialog. They outrank the learned weights: a hated series is
 * never suggested again, whatever its genre score says.
 */
@Serializable
data class TasteProfile(
    val genreWeights: Map<String, Double> = emptyMap(),
    val channelWeights: Map<String, Double> = emptyMap(),
    val verdicts: Map<String, String> = emptyMap(),
    val lastDecayMillis: Long = 0L,
) {
    fun verdictFor(title: String): Verdict =
        verdicts[title]?.let { runCatching { Verdict.valueOf(it) }.getOrNull() } ?: Verdict.NONE
}

class TasteStore(private val context: Context) {

    private val key = stringPreferencesKey("taste_profile_json")
    private val json = Json { ignoreUnknownKeys = true }

    val profile: Flow<TasteProfile> = context.tasteDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    suspend fun update(transform: (TasteProfile) -> TasteProfile) {
        context.tasteDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(transform(decode(prefs[key])))
        }
    }

    /** Sets (or with [Verdict.NONE] clears) the standing opinion of a series. */
    suspend fun setVerdict(title: String, verdict: Verdict) = update { profile ->
        profile.copy(
            verdicts = if (verdict == Verdict.NONE) {
                profile.verdicts - title
            } else {
                profile.verdicts + (title to verdict.name)
            },
        )
    }

    private fun decode(raw: String?): TasteProfile =
        raw?.let { runCatching { json.decodeFromString<TasteProfile>(it) }.getOrNull() } ?: TasteProfile()
}
