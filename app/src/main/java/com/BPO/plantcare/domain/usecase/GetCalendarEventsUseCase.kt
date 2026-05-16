package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.CalendarEvent
import com.BPO.plantcare.domain.model.CalendarEventType
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetCalendarEventsUseCase @Inject constructor(
    private val repository: PlantRepository,
) {
    operator fun invoke(
        windowDays: Int = WINDOW_DAYS_DEFAULT,
    ): Flow<Map<LocalDate, List<CalendarEvent>>> =
        repository.observeAll().map { plants ->
            buildEvents(plants, windowDays)
        }

    private fun buildEvents(
        plants: List<Plant>,
        windowDays: Int,
    ): Map<LocalDate, List<CalendarEvent>> {
        val today = LocalDate.now()
        val horizon = today.plusDays(windowDays.toLong())
        val events = mutableListOf<CalendarEvent>()

        plants.forEach { plant ->
            // Pasado: ultimo riego como evento puntual.
            plant.lastWateredAt?.let { ts ->
                val date = ts.toLocalDate()
                events += CalendarEvent(date, CalendarEventType.Watered, plant)
            }

            // Futuro: proximos riegos calculados desde la fecha de referencia.
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
