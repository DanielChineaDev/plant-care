package com.BPO.plantcare.core.work

import com.BPO.plantcare.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquesta el scheduler de WorkManager con las preferencias del usuario.
 * Punto unico para activar/desactivar notificaciones y cambiar la hora.
 */
@Singleton
class WateringReminderManager @Inject constructor(
    private val scheduler: WateringReminderScheduler,
    private val preferences: PreferencesRepository,
) {
    /** Lee las preferencias actuales y aplica el scheduling correspondiente. */
    suspend fun applyOnStartup() {
        val settings = preferences.settings.first()
        if (settings.notificationsEnabled) {
            scheduler.scheduleDaily(settings.reminderHour)
        } else {
            scheduler.cancel()
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
        if (enabled) {
            val hour = preferences.settings.first().reminderHour
            scheduler.scheduleDaily(hour)
        } else {
            scheduler.cancel()
        }
    }

    suspend fun setHour(hour: Int) {
        preferences.setReminderHour(hour)
        val settings = preferences.settings.first()
        if (settings.notificationsEnabled) {
            scheduler.scheduleDaily(settings.reminderHour)
        }
    }

    fun runNow() = scheduler.runNow()
}
