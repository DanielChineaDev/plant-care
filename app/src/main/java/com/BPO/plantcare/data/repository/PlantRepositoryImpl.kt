package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    private val dao: PlantDao,
) : PlantRepository {

    override fun observeAll(): Flow<List<Plant>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Plant?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun add(plant: Plant): Long = dao.insert(plant.toEntity())

    override suspend fun update(plant: Plant) = dao.update(plant.toEntity())

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun markWatered(id: Long, timestamp: Long) =
        dao.markWatered(id, timestamp)
}
