package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.local.dao.WateringLogDao
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.repository.WateringLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WateringLogRepositoryImpl @Inject constructor(
    private val dao: WateringLogDao,
) : WateringLogRepository {

    override fun observeAll(): Flow<List<WateringLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForPlant(plantId: Long): Flow<List<WateringLog>> =
        dao.observeForPlant(plantId).map { list -> list.map { it.toDomain() } }

    override suspend fun add(log: WateringLog): Long = dao.insert(log.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)
}
