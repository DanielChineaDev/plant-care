package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.repository.PlantRepository
import javax.inject.Inject

class MarkPlantWateredUseCase @Inject constructor(
    private val repository: PlantRepository,
) {
    suspend operator fun invoke(plantId: Long) {
        repository.markWatered(plantId)
    }
}
