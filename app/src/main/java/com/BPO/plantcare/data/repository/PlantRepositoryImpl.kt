package com.BPO.plantcare.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.BPO.plantcare.core.widget.WateringWidget
import com.BPO.plantcare.data.local.PlantCareDatabase
import com.BPO.plantcare.data.local.dao.PlantDao
import com.BPO.plantcare.data.local.dao.WateringLogDao
import com.BPO.plantcare.data.local.entity.WateringLogEntity
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PlantCareDatabase,
    private val plantDao: PlantDao,
    private val wateringLogDao: WateringLogDao,
) : PlantRepository {

    override fun observeAll(): Flow<List<Plant>> =
        plantDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Plant?> =
        plantDao.observeById(id).map { it?.toDomain() }

    override suspend fun add(plant: Plant): Long {
        val id = plantDao.insert(plant.toEntity())
        refreshWidget()
        return id
    }

    override suspend fun update(plant: Plant) {
        plantDao.update(plant.toEntity())
        refreshWidget()
    }

    override suspend fun delete(id: Long) {
        plantDao.delete(id)
        refreshWidget()
    }

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
        refreshWidget()
    }

    private suspend fun refreshWidget() {
        runCatching { WateringWidget().updateAll(context) }
    }
}
