package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.repository.PlantIdentificationRepository
import java.io.File
import javax.inject.Inject

class IdentifyPlantUseCase @Inject constructor(
    private val repository: PlantIdentificationRepository,
) {
    suspend operator fun invoke(image: File): Result<List<PlantSuggestion>> =
        repository.identify(image)
}
