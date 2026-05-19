package com.BPO.plantcare.domain.model

/**
 * Tipos de tarea de cuidado que el usuario puede programar para cada planta.
 *
 * Cada tipo trae una cadencia por defecto razonable (en dias). El user
 * puede personalizarla por planta o desactivar la tarea.
 *
 * IMPORTANTE: la entrada de string [storageKey] es la que se guarda en
 * Room. NO se debe cambiar a la ligera porque rompe la migracion de
 * datos existentes.
 *
 * Nota: el riego ("Water") existe ademas como columna en Plant
 * (`wateringIntervalDays` + `lastWateredAt`) para mantener compatibilidad
 * con el widget y el WateringReminderWorker actuales. Mientras tanto,
 * las tareas NUEVAS (Fertilize, Prune, etc.) viven en `plant_tasks` y
 * se gestionan desde aqui. En un sprint futuro se puede unificar todo
 * en `plant_tasks` y deprecar las columnas de Plant.
 */
enum class PlantTaskType(
    val storageKey: String,
    val emoji: String,
    val label: String,
    val defaultIntervalDays: Int,
) {
    Water(storageKey = "water", emoji = "💧", label = "Regar", defaultIntervalDays = 5),
    Fertilize(storageKey = "fertilize", emoji = "🌿", label = "Abonar", defaultIntervalDays = 30),
    Prune(storageKey = "prune", emoji = "✂️", label = "Podar", defaultIntervalDays = 90),
    Repot(storageKey = "repot", emoji = "🪴", label = "Trasplantar", defaultIntervalDays = 365),
    Rotate(storageKey = "rotate", emoji = "🔄", label = "Rotar", defaultIntervalDays = 14),
    CleanLeaves(
        storageKey = "clean_leaves",
        emoji = "🧽",
        label = "Limpiar hojas",
        defaultIntervalDays = 30,
    ),
    Fumigate(storageKey = "fumigate", emoji = "🪲", label = "Fumigar", defaultIntervalDays = 60);

    companion object {
        fun fromStorageKey(key: String?): PlantTaskType? =
            key?.let { k -> entries.firstOrNull { it.storageKey == k } }
    }
}

/**
 * Tarea de cuidado configurada para una planta.
 *
 * - [lastDoneAt] = null significa que nunca se ha realizado; se considera
 *   pendiente desde la fecha de alta de la planta para calculos de "vencida".
 * - [snoozedUntil] cuando != null pospone la tarea hasta ese instante. Se
 *   limpia al marcar la tarea como hecha o al editar el intervalo.
 */
data class PlantTask(
    val id: Long = 0L,
    val plantId: Long,
    val type: PlantTaskType,
    val intervalDays: Int,
    val lastDoneAt: Long?,
    val snoozedUntil: Long?,
    val enabled: Boolean,
    val createdAt: Long,
)

/**
 * Calcula cuando vence la siguiente ejecucion. Respeta snoozedUntil si es
 * mayor que el calculo normal.
 */
fun PlantTask.nextDueAt(plantAddedAt: Long): Long {
    val base = lastDoneAt ?: plantAddedAt
    val due = base + intervalDays.toLong() * 24L * 60L * 60L * 1000L
    val snooze = snoozedUntil
    return if (snooze != null && snooze > due) snooze else due
}

fun PlantTask.isDue(now: Long = System.currentTimeMillis(), plantAddedAt: Long): Boolean =
    enabled && nextDueAt(plantAddedAt) <= now
