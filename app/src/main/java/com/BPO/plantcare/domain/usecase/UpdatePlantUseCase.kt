package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import javax.inject.Inject

class UpdatePlantUseCase @Inject constructor(
    private val repository: PlantRepository,
) {
    suspend operator fun invoke(plant: Plant) {
        repository.update(plant)
    }
}
