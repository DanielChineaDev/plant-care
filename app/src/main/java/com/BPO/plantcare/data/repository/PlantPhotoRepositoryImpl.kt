package com.BPO.plantcare.data.repository

import com.BPO.plantcare.data.local.dao.PlantPhotoDao
import com.BPO.plantcare.data.local.entity.toDomain
import com.BPO.plantcare.data.local.entity.toEntity
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantPhotoRepositoryImpl @Inject constructor(
    private val dao: PlantPhotoDao,
) : PlantPhotoRepository {

    override fun observeForPlant(plantId: Long): Flow<List<PlantPhoto>> =
        dao.observeForPlant(plantId).map { list -> list.map { it.toDomain() } }

    override suspend fun getForPlant(plantId: Long): List<PlantPhoto> =
        dao.getForPlant(plantId).map { it.toDomain() }

    override suspend fun add(photo: PlantPhoto): Long = dao.insert(photo.toEntity())

    override suspend fun delete(photoId: Long) = dao.delete(photoId)

    override suspend fun getById(id: Long): PlantPhoto? = dao.getById(id)?.toDomain()
}
