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
import com.BPO.plantcare.data.sync.PlantCloudDataSource
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PlantCareDatabase,
    private val plantDao: PlantDao,
    private val wateringLogDao: WateringLogDao,
    private val firebaseAuth: FirebaseAuth,
    private val cloud: PlantCloudDataSource,
) : PlantRepository {

    // Las escrituras a la nube se hacen en un scope aparte para NO bloquear
    // la operacion local (Firestore offline deja la escritura pendiente hasta
    // reconectar; no queremos colgar add/update en ese caso).
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun pushUpsert(plant: Plant) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        syncScope.launch { runCatching { cloud.upsert(uid, plant) } }
    }

    private fun pushDelete(plantId: Long) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        syncScope.launch { runCatching { cloud.delete(uid, plantId) } }
    }

    override fun observeAll(): Flow<List<Plant>> =
        plantDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Plant?> =
        plantDao.observeById(id).map { it?.toDomain() }

    override suspend fun add(plant: Plant): Long {
        val id = plantDao.insert(plant.toEntity())
        pushUpsert(plant.copy(id = id))
        refreshWidget()
        return id
    }

    override suspend fun update(plant: Plant) {
        plantDao.update(plant.toEntity())
        pushUpsert(plant)
        refreshWidget()
    }

    override suspend fun delete(id: Long) {
        plantDao.delete(id)
        pushDelete(id)
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
        // Reflejamos el nuevo lastWateredAt en la nube.
        plantDao.getById(id)?.let { pushUpsert(it.toDomain()) }
        refreshWidget()
    }

    private suspend fun refreshWidget() {
        runCatching { WateringWidget().updateAll(context) }
    }
}
