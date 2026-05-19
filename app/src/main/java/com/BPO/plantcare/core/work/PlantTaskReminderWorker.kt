package com.BPO.plantcare.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.BPO.plantcare.core.notification.PlantTaskNotifier
import com.BPO.plantcare.domain.model.isDue
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import com.BPO.plantcare.domain.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker diario que recorre tareas (no-riego) vencidas y muestra una
 * notificacion por cada una con acciones "Hecho", "+1h" y "Manana".
 *
 * Si el usuario esta en modo viaje, omitimos las notifs igual que en
 * el WateringReminderWorker para no estresarle de lejos.
 */
@HiltWorker
class PlantTaskReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: PlantTaskRepository,
    private val plantRepository: PlantRepository,
    private val preferences: PreferencesRepository,
    private val notifier: PlantTaskNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val settings = preferences.settings.first()
        if (settings.isCurrentlyOnTrip(now)) return@runCatching Result.success()

        val plants = plantRepository.observeAll().first().associateBy { it.id }
        val tasks = taskRepository.observeAllEnabled().first()
        val due = tasks.filter { task ->
            val plant = plants[task.plantId] ?: return@filter false
            task.isDue(now, plant.addedAt)
        }
        due.forEach { task ->
            plants[task.plantId]?.let { plant -> notifier.showTaskReminder(task, plant) }
        }
        Result.success()
    }.getOrElse { Result.retry() }
}
