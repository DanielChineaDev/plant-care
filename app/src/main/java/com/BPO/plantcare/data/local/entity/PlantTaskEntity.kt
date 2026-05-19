package com.BPO.plantcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistencia de una tarea de cuidado por planta. UNIQUE(plantId, type) para
 * no duplicar configuraciones del mismo tipo en la misma planta.
 */
@Entity(
    tableName = "plant_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["plantId"]),
        Index(value = ["plantId", "type"], unique = true),
    ],
)
data class PlantTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    /** Storage key del PlantTaskType, p.ej. "water", "fertilize"... */
    val type: String,
    val intervalDays: Int,
    val lastDoneAt: Long?,
    val snoozedUntil: Long?,
    val enabled: Boolean,
    val createdAt: Long,
)
