package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import com.BPO.plantcare.domain.repository.PlantRepository
import javax.inject.Inject

class DeletePlantUseCase @Inject constructor(
    private val repository: PlantRepository,
    private val photoRepository: PlantPhotoRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(plant: Plant) {
        // Borra archivos fisicos: foto principal + diario completo.
        plant.userPhotoPath?.let { photoStorage.delete(it) }
        photoRepository.getForPlant(plant.id).forEach {
            photoStorage.delete(it.path)
        }
        // FK CASCADE elimina entradas de watering_logs y plant_photos.
        repository.delete(plant.id)
    }
}
