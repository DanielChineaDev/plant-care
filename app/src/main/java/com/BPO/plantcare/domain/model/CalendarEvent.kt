package com.BPO.plantcare.domain.model

import java.time.LocalDate

enum class CalendarEventType { Watered, WateringDue }

data class CalendarEvent(
    val date: LocalDate,
    val type: CalendarEventType,
    val plant: Plant,
)
