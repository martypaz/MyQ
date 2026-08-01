package com.martypaz.myq.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

/** Who is watching, plus the preferences shown on the Settings screen. */
data class Profile(
    val firstName: String? = null,
    val defaultLeadHours: Int = 1,
)

class ProfileStore(private val context: Context) {

    private val nameKey = stringPreferencesKey("first_name")
    private val leadKey = intPreferencesKey("default_lead_hours")

    val profile: Flow<Profile> = context.profileDataStore.data.map { prefs ->
        Profile(
            firstName = prefs[nameKey]?.takeIf { it.isNotBlank() },
            defaultLeadHours = prefs[leadKey] ?: 1,
        )
    }

    suspend fun setFirstName(name: String) {
        context.profileDataStore.edit { it[nameKey] = name.trim() }
    }

    suspend fun setDefaultLeadHours(hours: Int) {
        context.profileDataStore.edit { it[leadKey] = hours }
    }
}
