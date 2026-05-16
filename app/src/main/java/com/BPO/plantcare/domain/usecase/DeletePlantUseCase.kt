package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import javax.inject.Inject

class DeletePlantUseCase @Inject constructor(
    private val repository: PlantRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(plant: Plant) {
        plant.userPhotoPath?.let { photoStorage.delete(it) }
        repository.delete(plant.id)
    }
}
