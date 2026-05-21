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
import java.io.File
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
        syncScope.launch {
            runCatching {
                var toSync = plant
                // Si hay una foto local que aun no se ha subido a la nube, la
                // subimos y guardamos su URL (en local y en la nube) para que
                // sobreviva al cambio de dispositivo.
                val localPhoto = plant.userPhotoPath
                    ?.let { File(it) }
                    ?.takeIf { it.exists() }
                if (localPhoto != null && plant.userPhotoUrl == null) {
                    val url = cloud.uploadMainPhoto(uid, plant.id, localPhoto)
                    toSync = plant.copy(userPhotoUrl = url)
                    plantDao.getById(plant.id)?.let {
                        plantDao.update(it.copy(userPhotoUrl = url))
                    }
                }
                cloud.upsert(uid, toSync)
            }
        }
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
        // Si cambia la foto local, invalidamos la URL en la nube para que
        // pushUpsert vuelva a subir la nueva.
        val existing = plantDao.getById(plant.id)?.toDomain()
        val photoChanged = existing?.userPhotoPath != plant.userPhotoPath
        val toSave = if (photoChanged) plant.copy(userPhotoUrl = null) else plant
        plantDao.update(toSave.toEntity())
        pushUpsert(toSave)
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
