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
    /**
     * URL de la foto del usuario subida a Firebase Storage. Permite recuperar
     * la foto al cambiar de dispositivo, cuando [userPhotoPath] (ruta local)
     * ya no existe.
     */
    val userPhotoUrl: String? = null,
    val addedAt: Long,
    val lastWateredAt: Long?,
    val wateringIntervalDays: Int,
    val notes: String?,
    val isOutdoor: Boolean? = null,
    /** Ubicacion/habitacion de la planta (ej. "Salon", "Cocina"). */
    val room: String? = null,
) {
    val displayName: String
        get() = nickname?.takeIf { it.isNotBlank() }
            ?: commonName?.takeIf { it.isNotBlank() }
            ?: scientificName
}

/**
 * Modelo de imagen preferido para mostrar la planta: el archivo local si
 * existe, si no la URL en la nube (otro dispositivo), y por ultimo la imagen
 * de referencia del catalogo. Apto para pasar a Coil (acepta File o String).
 */
fun Plant.photoModel(): Any? =
    userPhotoPath?.let { java.io.File(it) }?.takeIf { it.exists() }
        ?: userPhotoUrl
        ?: referenceImageUrl

enum class PlantStatus(
    val emoji: String,
    @androidx.annotation.StringRes val labelRes: Int,
) {
    Healthy(emoji = "😊", labelRes = com.BPO.plantcare.R.string.status_healthy),
    Attention(emoji = "😐", labelRes = com.BPO.plantcare.R.string.status_attention),
    Thirsty(emoji = "🥵", labelRes = com.BPO.plantcare.R.string.status_thirsty),
    NotWatered(emoji = "🌱", labelRes = com.BPO.plantcare.R.string.status_not_watered),
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

/**
 * True si la planta deberia regarse hoy (intervalo cumplido desde el ultimo riego;
 * si nunca se rego, desde la fecha de alta).
 */
fun Plant.needsWatering(now: Long = System.currentTimeMillis()): Boolean {
    val reference = lastWateredAt ?: addedAt
    val intervalMs = wateringIntervalDays.toLong() * 24L * 60L * 60L * 1000L
    return (now - reference) >= intervalMs
}

/**
 * Timestamp del proximo riego previsto: ultimo riego (o alta si nunca se
 * rego) + intervalo. Usado para ordenar "por proximo riego".
 */
fun Plant.nextWateringAt(): Long {
    val reference = lastWateredAt ?: addedAt
    return reference + wateringIntervalDays.toLong() * 24L * 60L * 60L * 1000L
}
