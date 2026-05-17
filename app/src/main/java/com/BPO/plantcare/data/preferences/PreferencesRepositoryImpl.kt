package com.BPO.plantcare.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.BPO.plantcare.domain.model.UserSettings
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "plantcare_prefs")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    override val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 10,
            travelEnabled = prefs[Keys.TRAVEL_ENABLED] ?: false,
            travelStart = prefs[Keys.TRAVEL_START],
            travelEnd = prefs[Keys.TRAVEL_END],
        )
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23) }
    }

    override suspend fun setTravelEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TRAVEL_ENABLED] = enabled }
    }

    override suspend fun setTravelRange(start: Long?, end: Long?) {
        context.dataStore.edit { prefs ->
            if (start == null) prefs.remove(Keys.TRAVEL_START) else prefs[Keys.TRAVEL_START] = start
            if (end == null) prefs.remove(Keys.TRAVEL_END) else prefs[Keys.TRAVEL_END] = end
        }
    }

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val TRAVEL_ENABLED = booleanPreferencesKey("travel_enabled")
        val TRAVEL_START = longPreferencesKey("travel_start")
        val TRAVEL_END = longPreferencesKey("travel_end")
    }
}
