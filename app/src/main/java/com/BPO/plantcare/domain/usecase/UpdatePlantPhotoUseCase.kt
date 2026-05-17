package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import java.io.File
import javax.inject.Inject

/**
 * Persiste [source] como nueva foto del usuario en almacenamiento interno
 * y borra la anterior (si la habia) para evitar archivos huerfanos.
 */
class UpdatePlantPhotoUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(plant: Plant, source: File): Result<Unit> = runCatching {
        val newPath = photoStorage.persist(source)
        plant.userPhotoPath?.let { runCatching { photoStorage.delete(it) } }
        plantRepository.update(plant.copy(userPhotoPath = newPath))
    }
}
