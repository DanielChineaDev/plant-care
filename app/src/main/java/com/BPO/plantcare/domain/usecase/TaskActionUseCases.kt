package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.PlantTaskType
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import javax.inject.Inject

class MarkTaskDoneUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    suspend operator fun invoke(taskId: Long) {
        repository.markDone(taskId)
    }
}

class SnoozeTaskUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    /**
     * Pospone la tarea [minutes] minutos. Util desde la accion de la
     * notificacion ("Recordar en 1h", "Recordar manana").
     */
    suspend operator fun invoke(taskId: Long, minutes: Int) {
        val until = System.currentTimeMillis() + minutes.toLong() * 60_000L
        repository.snooze(taskId, until)
    }
}

class EnableTaskUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    suspend operator fun invoke(
        plantId: Long,
        type: PlantTaskType,
        intervalDays: Int = type.defaultIntervalDays,
    ): Long = repository.enableTask(plantId, type, intervalDays)
}

class DisableTaskUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    suspend operator fun invoke(taskId: Long) {
        repository.disableTask(taskId)
    }
}

class UpdateTaskIntervalUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    suspend operator fun invoke(taskId: Long, days: Int) {
        repository.updateInterval(taskId, days)
    }
}
