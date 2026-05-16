package com.BPO.plantcare.domain.model

data class Plant(
    val id: Long = 0,
    val nickname: String?,
    val scientificName: String,
    val commonName: String?,
    val family: String?,
    val genus: String?,
    val referenceImageUrl: String?,
    val userPhotoPath: String?,
    val addedAt: Long,
    val lastWateredAt: Long?,
    val wateringIntervalDays: Int,
    val notes: String?,
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() }
            ?: commonName?.takeIf { it.isNotBlank() }
            ?: scientificName
}

enum class PlantStatus(val emoji: String, val label: String) {
    Healthy(emoji = "😊", label = "Feliz"),         // 😊
    Attention(emoji = "😐", label = "Atenta"),       // 😐
    Thirsty(emoji = "🥵", label = "Sedienta"),       // 🥵
    NotWatered(emoji = "🌱", label = "Sin regar aún"), // 🌱
}

/**
 * Calcula el estado de una planta segun cuanto tiempo ha pasado desde el ultimo riego.
 *
 * - Sin regar todavia -> NotWatered
 * - Dentro del intervalo -> Healthy
 * - Hasta un 50% mas alla del intervalo -> Attention
 * - Mas alla -> Thirsty
 */
fun Plant.status(now: Long = System.currentTimeMillis()): PlantStatus {
    val last = lastWateredAt ?: return PlantStatus.NotWatered
    val intervalMs = wateringIntervalDays.toLong() * 24L * 60L * 60L * 1000L
    val elapsed = now - last
    return when {
        elapsed < intervalMs -> PlantStatus.Healthy
        elapsed < intervalMs + intervalMs / 2 -> PlantStatus.Attention
        else -> PlantStatus.Thirsty
    }
}
