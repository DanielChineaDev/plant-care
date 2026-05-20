package com.BPO.plantcare.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            weatherAware = prefs[Keys.WEATHER_AWARE] ?: false,
            latitude = prefs[Keys.LATITUDE],
            longitude = prefs[Keys.LONGITUDE],
            locationUpdatedAt = prefs[Keys.LOCATION_UPDATED_AT],
            seasonalAdjustEnabled = prefs[Keys.SEASONAL_ADJUST] ?: true,
            themePalette = prefs[Keys.THEME_PALETTE] ?: "green",
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
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

    override suspend fun setWeatherAware(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WEATHER_AWARE] = enabled }
    }

    override suspend fun setLocation(latitude: Double?, longitude: Double?, updatedAt: Long?) {
        context.dataStore.edit { prefs ->
            if (latitude == null) prefs.remove(Keys.LATITUDE) else prefs[Keys.LATITUDE] = latitude
            if (longitude == null) prefs.remove(Keys.LONGITUDE) else prefs[Keys.LONGITUDE] = longitude
            if (updatedAt == null) prefs.remove(Keys.LOCATION_UPDATED_AT)
            else prefs[Keys.LOCATION_UPDATED_AT] = updatedAt
        }
    }

    override suspend fun setSeasonalAdjustEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SEASONAL_ADJUST] = enabled }
    }

    override suspend fun setThemePalette(palette: String) {
        context.dataStore.edit { it[Keys.THEME_PALETTE] = palette }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val TRAVEL_ENABLED = booleanPreferencesKey("travel_enabled")
        val TRAVEL_START = longPreferencesKey("travel_start")
        val TRAVEL_END = longPreferencesKey("travel_end")
        val WEATHER_AWARE = booleanPreferencesKey("weather_aware")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val LOCATION_UPDATED_AT = longPreferencesKey("location_updated_at")
        val SEASONAL_ADJUST = booleanPreferencesKey("seasonal_adjust")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
