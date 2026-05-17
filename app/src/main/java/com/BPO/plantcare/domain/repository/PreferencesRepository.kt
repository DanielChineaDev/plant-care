package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val settings: Flow<UserSettings>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
}
