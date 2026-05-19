package com.BPO.plantcare.core.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa el [PlantTaskReminderWorker] una vez al dia a la misma hora que
 * el recordatorio de riego (asi el usuario solo configura una hora). El
 * scheduler vive aparte para no acoplar el ciclo de tareas al de riego.
 */
@Singleton
class PlantTaskReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleDaily(hour: Int, resetSchedule: Boolean = false) {
        val safeHour = hour.coerceIn(0, 23)
        val request = PeriodicWorkRequestBuilder<PlantTaskReminderWorker>(24, TimeUnit.HOURS)
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
        private const val UNIQUE_WORK_NAME = "plant_tasks_reminder_daily"
    }
}
