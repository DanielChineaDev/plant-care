package com.BPO.plantcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.BPO.plantcare.domain.model.WateringLog

@Entity(
    tableName = "watering_logs",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId"), Index("timestamp")],
)
data class WateringLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val timestamp: Long,
    val note: String?,
)

fun WateringLogEntity.toDomain(): WateringLog = WateringLog(
    id = id,
    plantId = plantId,
    timestamp = timestamp,
    note = note,
)

fun WateringLog.toEntity(): WateringLogEntity = WateringLogEntity(
    id = id,
    plantId = plantId,
    timestamp = timestamp,
    note = note,
)
