package com.BPO.plantcare.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.BPO.plantcare.domain.model.PlantPhoto

@Entity(
    tableName = "plant_photos",
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
data class PlantPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val path: String,
    val timestamp: Long,
    val note: String?,
    val remoteUrl: String? = null,
)

fun PlantPhotoEntity.toDomain(): PlantPhoto = PlantPhoto(
    id = id,
    plantId = plantId,
    path = path,
    timestamp = timestamp,
    note = note,
    remoteUrl = remoteUrl,
)

fun PlantPhoto.toEntity(): PlantPhotoEntity = PlantPhotoEntity(
    id = id,
    plantId = plantId,
    path = path,
    timestamp = timestamp,
    note = note,
    remoteUrl = remoteUrl,
)
