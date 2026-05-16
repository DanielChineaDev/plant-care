package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.repository.PlantRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlantUseCase @Inject constructor(
    private val repository: PlantRepository,
) {
    operator fun invoke(plantId: Long): Flow<Plant?> = repository.observeById(plantId)
}
