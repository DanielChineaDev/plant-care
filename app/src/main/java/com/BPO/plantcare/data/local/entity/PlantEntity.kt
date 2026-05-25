package com.BPO.plantcare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.BPO.plantcare.domain.model.Plant

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String?,
    val scientificName: String,
    val commonName: String?,
    val family: String?,
    val genus: String?,
    val referenceImageUrl: String?,
    val userPhotoPath: String?,
    val userPhotoUrl: String? = null,
    val addedAt: Long,
    val lastWateredAt: Long?,
    val wateringIntervalDays: Int,
    val notes: String?,
    val isOutdoor: Boolean? = null,
    /** Ubicacion/habitacion donde esta la planta (salon, cocina...). */
    val room: String? = null,
    /** Diario fotografico visible publicamente. */
    val photosPublic: Boolean = false,
    /** Notas visibles publicamente. */
    val notesPublic: Boolean = false,
)

fun PlantEntity.toDomain(): Plant = Plant(
    id = id,
    nickname = nickname,
    scientificName = scientificName,
    commonName = commonName,
    family = family,
    genus = genus,
    referenceImageUrl = referenceImageUrl,
    userPhotoPath = userPhotoPath,
    userPhotoUrl = userPhotoUrl,
    addedAt = addedAt,
    lastWateredAt = lastWateredAt,
    wateringIntervalDays = wateringIntervalDays,
    notes = notes,
    isOutdoor = isOutdoor,
    room = room,
    photosPublic = photosPublic,
    notesPublic = notesPublic,
)

fun Plant.toEntity(): PlantEntity = PlantEntity(
    id = id,
    nickname = nickname,
    scientificName = scientificName,
    commonName = commonName,
    family = family,
    genus = genus,
    referenceImageUrl = referenceImageUrl,
    userPhotoPath = userPhotoPath,
    userPhotoUrl = userPhotoUrl,
    addedAt = addedAt,
    lastWateredAt = lastWateredAt,
    wateringIntervalDays = wateringIntervalDays,
    notes = notes,
    isOutdoor = isOutdoor,
    room = room,
    photosPublic = photosPublic,
    notesPublic = notesPublic,
)
