package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import javax.inject.Inject

class DeletePlantPhotoUseCase @Inject constructor(
    private val repository: PlantPhotoRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(photoId: Long) {
        val photo = repository.getById(photoId) ?: return
        photoStorage.delete(photo.path)
        repository.delete(photoId)
    }
}
