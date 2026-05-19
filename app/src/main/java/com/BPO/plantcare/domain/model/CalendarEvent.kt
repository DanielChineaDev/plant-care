package com.BPO.plantcare.domain.model

import java.time.LocalDate

enum class CalendarEventType {
    /** Riego ya realizado (entrada del historial). */
    Watered,

    /** Riego pendiente / vencido. */
    WateringDue,

    /** Otra tarea de cuidado pendiente / vencida (abonar, podar...). */
    TaskDue,
}

data class CalendarEvent(
    val date: LocalDate,
    val type: CalendarEventType,
    val plant: Plant,
    /** Solo presente cuando [type] == TaskDue. */
    val task: PlantTask? = null,
)
