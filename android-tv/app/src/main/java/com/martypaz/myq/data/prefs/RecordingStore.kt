package com.martypaz.myq.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.martypaz.myq.data.model.RecordEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recordingDataStore by preferencesDataStore(name = "recordings")

/** Persists the user's record list (see [RecordEntry] for what that means). */
class RecordingStore(private val context: Context) {

    private val key = stringPreferencesKey("recordings_json")
    private val json = Json { ignoreUnknownKeys = true }

    val recordings: Flow<List<RecordEntry>> = context.recordingDataStore.data.map { prefs ->
        decode(prefs[key]).sortedBy { it.startMillis }
    }

    suspend fun current(): List<RecordEntry> = recordings.first()

    suspend fun upsert(entry: RecordEntry) {
        context.recordingDataStore.edit { prefs ->
            val next = decode(prefs[key]).filterNot { it.programmeId == entry.programmeId } + entry
            prefs[key] = json.encodeToString(next)
        }
    }

    suspend fun remove(programmeId: String) {
        context.recordingDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filterNot { it.programmeId == programmeId })
        }
    }

    /** Drops the series marker and every saved episode of that series. */
    suspend fun removeSeries(title: String) {
        context.recordingDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filterNot { it.title == title })
        }
    }

    private fun decode(raw: String?): List<RecordEntry> =
        raw?.let { runCatching { json.decodeFromString<List<RecordEntry>>(it) }.getOrNull() } ?: emptyList()
}
