package com.martypaz.myq.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.martypaz.myq.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

/** Who is watching, plus the preferences shown on the Settings screen. */
data class Profile(
    val firstName: String? = null,
    val defaultLeadMinutes: Int = Reminder.DEFAULT_LEAD_MINUTES,
    /** Used to resolve the transmitter region for Freeview listings. */
    val postcode: String? = null,
    val networkId: Int? = null,
    val regionName: String? = null,
)

class ProfileStore(private val context: Context) {

    private val nameKey = stringPreferencesKey("first_name")
    private val leadMinutesKey = intPreferencesKey("default_lead_minutes")

    /** What the preference was called when it could only be whole hours. */
    private val legacyLeadHoursKey = intPreferencesKey("default_lead_hours")
    private val postcodeKey = stringPreferencesKey("postcode")
    private val networkIdKey = intPreferencesKey("network_id")
    private val regionKey = stringPreferencesKey("region_name")

    val profile: Flow<Profile> = context.profileDataStore.data.map { prefs ->
        Profile(
            firstName = prefs[nameKey]?.takeIf { it.isNotBlank() },
            defaultLeadMinutes = prefs[leadMinutesKey]
                ?: prefs[legacyLeadHoursKey]?.times(Reminder.MINUTES_PER_HOUR)
                ?: Reminder.DEFAULT_LEAD_MINUTES,
            postcode = prefs[postcodeKey]?.takeIf { it.isNotBlank() },
            networkId = prefs[networkIdKey],
            regionName = prefs[regionKey]?.takeIf { it.isNotBlank() },
        )
    }

    suspend fun setFirstName(name: String) {
        context.profileDataStore.edit { it[nameKey] = name.trim() }
    }

    suspend fun setRegion(postcode: String, networkId: Int, regionName: String) {
        context.profileDataStore.edit {
            it[postcodeKey] = postcode.trim()
            it[networkIdKey] = networkId
            it[regionKey] = regionName
        }
    }

    suspend fun setDefaultLeadMinutes(minutes: Int) {
        context.profileDataStore.edit { it[leadMinutesKey] = minutes }
    }
}
