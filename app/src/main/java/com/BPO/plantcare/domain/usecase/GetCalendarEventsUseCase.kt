package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.CalendarEvent
import com.BPO.plantcare.domain.model.CalendarEventType
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.model.nextDueAt
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import com.BPO.plantcare.domain.repository.WateringLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetCalendarEventsUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val logRepository: WateringLogRepository,
    private val taskRepository: PlantTaskRepository,
) {
    operator fun invoke(
        windowDays: Int = WINDOW_DAYS_DEFAULT,
    ): Flow<Map<LocalDate, List<CalendarEvent>>> =
        combine(
            plantRepository.observeAll(),
            logRepository.observeAll(),
            taskRepository.observeAllEnabled(),
        ) { plants, logs, tasks ->
            buildEvents(plants, logs, tasks, windowDays)
        }

    private fun buildEvents(
        plants: List<Plant>,
        logs: List<WateringLog>,
        tasks: List<PlantTask>,
        windowDays: Int,
    ): Map<LocalDate, List<CalendarEvent>> {
        val plantById = plants.associateBy { it.id }
        val today = LocalDate.now()
        val horizon = today.plusDays(windowDays.toLong())
        val events = mutableListOf<CalendarEvent>()

        // Pasado: todos los riegos historicos.
        logs.forEach { log ->
            val plant = plantById[log.plantId] ?: return@forEach
            events += CalendarEvent(log.timestamp.toLocalDate(), CalendarEventType.Watered, plant)
        }

        // Futuro: proximos riegos calculados desde la fecha de referencia.
        plants.forEach { plant ->
            val refTs = plant.lastWateredAt ?: plant.addedAt
            val refDate = refTs.toLocalDate()
            val interval = plant.wateringIntervalDays.toLong().coerceAtLeast(1)
            var n = 1L
            while (true) {
                val next = refDate.plusDays(interval * n)
                if (next.isAfter(horizon)) break
                events += CalendarEvent(next, CalendarEventType.WateringDue, plant)
                n++
            }
        }

        // Tareas no-riego: proyectamos ocurrencias hasta el horizonte. La
        // primera siempre se anade (puede ser hoy o atrasada).
        tasks.forEach { task ->
            val plant = plantById[task.plantId] ?: return@forEach
            val interval = task.intervalDays.toLong().coerceAtLeast(1)
            val firstDueDate = task.nextDueAt(plant.addedAt).toLocalDate()
            var current = firstDueDate
            while (!current.isAfter(horizon)) {
                events += CalendarEvent(
                    date = current,
                    type = CalendarEventType.TaskDue,
                    plant = plant,
                    task = task,
                )
                current = current.plusDays(interval)
            }
        }
        return events.groupBy { it.date }
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        const val WINDOW_DAYS_DEFAULT = 120
    }
}
