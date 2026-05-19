package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.model.PlantTaskType
import kotlinx.coroutines.flow.Flow

interface PlantTaskRepository {

    /** Tareas configuradas (enabled o no) para una planta. */
    fun observeForPlant(plantId: Long): Flow<List<PlantTask>>

    /** Todas las tareas activas en el sistema. Util para "Tareas de hoy". */
    fun observeAllEnabled(): Flow<List<PlantTask>>

    /**
     * Crea (o upsertea) un task de un tipo dado para una planta. Si ya existe,
     * conserva sus valores y solo cambia enabled = true.
     */
    suspend fun enableTask(
        plantId: Long,
        type: PlantTaskType,
        intervalDays: Int = type.defaultIntervalDays,
    ): Long

    suspend fun disableTask(taskId: Long)

    suspend fun markDone(taskId: Long, now: Long = System.currentTimeMillis())

    /** Snooze (posponer): mueve la fecha vencimiento a [until]. */
    suspend fun snooze(taskId: Long, until: Long)

    suspend fun updateInterval(taskId: Long, days: Int)
}
