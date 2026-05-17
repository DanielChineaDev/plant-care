package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.PlantPhoto
import com.BPO.plantcare.domain.repository.PlantPhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlantPhotosUseCase @Inject constructor(
    private val repository: PlantPhotoRepository,
) {
    operator fun invoke(plantId: Long): Flow<List<PlantPhoto>> =
        repository.observeForPlant(plantId)
}
