package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.local.dao.PlantPhotoDao
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.data.sync.PlantPhotoCloudDataSource
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlantPhotoRepositoryImpl @Inject constructor(
    private val dao: PlantPhotoDao,
    private val firebaseAuth: FirebaseAuth,
    private val cloud: PlantPhotoCloudDataSource,
) : PlantPhotoRepository {

    // Subidas a la nube en un scope aparte para no bloquear la operacion local.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeForPlant(plantId: Long): Flow<List<PlantPhoto>> =
        dao.observeForPlant(plantId).map { list -> list.map { it.toDomain() } }

    override suspend fun getForPlant(plantId: Long): List<PlantPhoto> =
        dao.getForPlant(plantId).map { it.toDomain() }

    override suspend fun add(photo: PlantPhoto): Long {
        val id = dao.insert(photo.toEntity())
        pushUpload(photo.copy(id = id))
        return id
    }

    override suspend fun delete(photoId: Long) {
        val photo = dao.getById(photoId)?.toDomain()
        dao.delete(photoId)
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null && photo != null) {
            syncScope.launch {
                runCatching { cloud.delete(uid, photo.plantId, photoId) }
            }
        }
    }

    override suspend fun getById(id: Long): PlantPhoto? = dao.getById(id)?.toDomain()

    /** Sube la imagen a Storage y guarda su URL en local + Firestore. */
    private fun pushUpload(photo: PlantPhoto) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (photo.path.isBlank()) return
        syncScope.launch {
            runCatching {
                val url = cloud.upsert(uid, photo)
                // dao.insert usa REPLACE: reinsertar con el mismo id actualiza.
                dao.getById(photo.id)?.let { dao.insert(it.copy(remoteUrl = url)) }
            }
        }
    }
}
