package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.CalendarEvent
import com.BPO.plantcare.domain.model.CalendarEventType
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.repository.PlantRepository
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
) {
    operator fun invoke(
        windowDays: Int = WINDOW_DAYS_DEFAULT,
    ): Flow<Map<LocalDate, List<CalendarEvent>>> =
        combine(
            plantRepository.observeAll(),
            logRepository.observeAll(),
        ) { plants, logs ->
            buildEvents(plants, logs, windowDays)
        }

    private fun buildEvents(
        plants: List<Plant>,
        logs: List<WateringLog>,
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
        return events.groupBy { it.date }
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        const val WINDOW_DAYS_DEFAULT = 120
    }
}
