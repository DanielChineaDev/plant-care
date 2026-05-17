package com.BPO.plantcare.domain.model

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 10,
)
