package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun observeAll(): Flow<List<Plant>>
    fun observeById(id: Long): Flow<Plant?>
    suspend fun add(plant: Plant): Long
    suspend fun update(plant: Plant)
    suspend fun delete(id: Long)
    suspend fun markWatered(id: Long, timestamp: Long = System.currentTimeMillis())
}
