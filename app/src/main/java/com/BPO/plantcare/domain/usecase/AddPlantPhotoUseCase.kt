package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import java.io.File
import javax.inject.Inject

class AddPlantPhotoUseCase @Inject constructor(
    private val repository: PlantPhotoRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(
        plantId: Long,
        source: File,
        note: String? = null,
    ): Result<Long> = runCatching {
        val savedPath = photoStorage.persist(source)
        repository.add(
            PlantPhoto(
                plantId = plantId,
                path = savedPath,
                timestamp = System.currentTimeMillis(),
                note = note,
            ),
        )
    }
}
