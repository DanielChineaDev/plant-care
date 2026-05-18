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
            // En arranque: no resetear, respetamos el periodo ya programado.
            scheduler.scheduleDaily(settings.reminderHour, resetSchedule = false)
        } else {
            scheduler.cancel()
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        preferences.setNotificationsEnabled(enabled)
        if (enabled) {
            val hour = preferences.settings.first().reminderHour
            // Al reactivar las notif queremos empezar ya.
            scheduler.scheduleDaily(hour, resetSchedule = true)
        } else {
            scheduler.cancel()
        }
    }

    suspend fun setHour(hour: Int) {
        preferences.setReminderHour(hour)
        val settings = preferences.settings.first()
        if (settings.notificationsEnabled) {
            // Cambio explicito de hora: CANCEL_AND_REENQUEUE para que el
            // nuevo initialDelay se aplique YA (de lo contrario con UPDATE
            // el cambio no entraria hasta el siguiente ciclo de 24h).
            scheduler.scheduleDaily(settings.reminderHour, resetSchedule = true)
        }
    }

    fun runNow() = scheduler.runNow()
}
