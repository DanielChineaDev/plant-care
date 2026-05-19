package com.BPO.plantcare.domain.model

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 10,
    val travelEnabled: Boolean = false,
    val travelStart: Long? = null,
    val travelEnd: Long? = null,
    val weatherAware: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUpdatedAt: Long? = null,
    /**
     * Si esta activo, ajustamos el intervalo de riego segun la estacion
     * (15% mas frecuente en verano, 50% menos en invierno). Aplica a
     * needsWatering y status; el intervalo guardado en Plant no cambia.
     */
    val seasonalAdjustEnabled: Boolean = true,
) {
    /** True si las notificaciones deben suprimirse ahora por estar de viaje. */
    fun isCurrentlyOnTrip(now: Long = System.currentTimeMillis()): Boolean {
        if (!travelEnabled) return false
        val start = travelStart ?: return false
        val end = travelEnd ?: return false
        return now in start..end
    }

    val hasLocation: Boolean get() = latitude != null && longitude != null
}
