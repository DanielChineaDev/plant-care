package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.WateringLog
import kotlinx.coroutines.flow.Flow

interface WateringLogRepository {
    fun observeAll(): Flow<List<WateringLog>>
    fun observeForPlant(plantId: Long): Flow<List<WateringLog>>
    suspend fun delete(id: Long)
}
