package com.BPO.plantcare.domain.model

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 10,
    val travelEnabled: Boolean = false,
    val travelStart: Long? = null,
    val travelEnd: Long? = null,
) {
    /** True si las notificaciones deben suprimirse ahora por estar de viaje. */
    fun isCurrentlyOnTrip(now: Long = System.currentTimeMillis()): Boolean {
        if (!travelEnabled) return false
        val start = travelStart ?: return false
        val end = travelEnd ?: return false
        return now in start..end
    }
}
