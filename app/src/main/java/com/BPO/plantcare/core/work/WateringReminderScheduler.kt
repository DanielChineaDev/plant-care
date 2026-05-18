package com.BPO.plantcare.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WateringReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Programa una comprobacion diaria a la [hour] indicada con WorkManager.
     *
     * [resetSchedule]:
     *  - true  -> CANCEL_AND_REENQUEUE: respeta el nuevo initialDelay, asi
     *    el cambio explicito de hora desde Perfil entra ya hoy.
     *  - false -> UPDATE: mantiene el periodo en curso. Para
     *    [applyOnStartup] donde no queremos cambiar la cadencia si ya
     *    estaba programada.
     */
    fun scheduleDaily(hour: Int, resetSchedule: Boolean = false) {
        val safeHour = hour.coerceIn(0, 23)
        val request = PeriodicWorkRequestBuilder<WateringReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayUntilHour(safeHour), TimeUnit.MILLISECONDS)
            .build()
        val policy = if (resetSchedule) {
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        } else {
            ExistingPeriodicWorkPolicy.UPDATE
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            policy,
            request,
        )
    }

    /** Lanza una comprobacion inmediata (util para boton de prueba). */
    fun runNow() {
        val request = OneTimeWorkRequestBuilder<WateringReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "${UNIQUE_WORK_NAME}_oneshot",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun initialDelayUntilHour(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "watering_reminder_daily"
    }
}
