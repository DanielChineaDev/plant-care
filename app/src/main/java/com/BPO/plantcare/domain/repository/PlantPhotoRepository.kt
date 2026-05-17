package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantPhoto
import kotlinx.coroutines.flow.Flow

interface PlantPhotoRepository {
    fun observeForPlant(plantId: Long): Flow<List<PlantPhoto>>
    suspend fun getForPlant(plantId: Long): List<PlantPhoto>
    suspend fun add(photo: PlantPhoto): Long
    suspend fun delete(photoId: Long)
    suspend fun getById(id: Long): PlantPhoto?
}
