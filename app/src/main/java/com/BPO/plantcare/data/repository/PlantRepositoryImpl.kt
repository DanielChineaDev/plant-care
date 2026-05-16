package com.BPO.plantcare.data.repository

import androidx.room.withTransaction
import com.BPO.plantcare.data.local.PlantCareDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.WateringLogDao
import com.BPO.plantcare.data.local.entity.WateringLogEntity
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    private val database: PlantCareDatabase,
    private val plantDao: PlantDao,
    private val wateringLogDao: WateringLogDao,
) : PlantRepository {

    override fun observeAll(): Flow<List<Plant>> =
        plantDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Plant?> =
        plantDao.observeById(id).map { it?.toDomain() }

    override suspend fun add(plant: Plant): Long = plantDao.insert(plant.toEntity())

    override suspend fun update(plant: Plant) = plantDao.update(plant.toEntity())

    override suspend fun delete(id: Long) = plantDao.delete(id)

    /**
     * Registra el riego: inserta una entrada en watering_logs y actualiza
     * lastWateredAt del plant, todo en una unica transaccion.
     */
    override suspend fun markWatered(id: Long, timestamp: Long) {
        database.withTransaction {
            plantDao.markWatered(id, timestamp)
            wateringLogDao.insert(
                WateringLogEntity(plantId = id, timestamp = timestamp, note = null),
            )
        }
    }
}
