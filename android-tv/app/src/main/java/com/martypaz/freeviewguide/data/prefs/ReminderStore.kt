package com.martypaz.freeviewguide.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.martypaz.freeviewguide.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.reminderDataStore by preferencesDataStore(name = "reminders")

/** Persists the user's reminders as a JSON list in Preferences DataStore. */
class ReminderStore(private val context: Context) {

    private val key = stringPreferencesKey("reminders_json")
    private val json = Json { ignoreUnknownKeys = true }

    val reminders: Flow<List<Reminder>> = context.reminderDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    suspend fun current(): List<Reminder> = reminders.first()

    suspend fun upsert(reminder: Reminder) {
        context.reminderDataStore.edit { prefs ->
            val next = decode(prefs[key])
                .filterNot { it.programmeId == reminder.programmeId } + reminder
            prefs[key] = json.encodeToString(next)
        }
    }

    suspend fun remove(programmeId: String) {
        context.reminderDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filterNot { it.programmeId == programmeId })
        }
    }

    /** Drop reminders whose programme has already started. */
    suspend fun prune(now: Long = System.currentTimeMillis()) {
        context.reminderDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filter { it.startMillis > now })
        }
    }

    private fun decode(raw: String?): List<Reminder> =
        raw?.let { runCatching { json.decodeFromString<List<Reminder>>(it) }.getOrNull() } ?: emptyList()
}
