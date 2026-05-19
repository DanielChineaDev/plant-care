package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.PlantTask
import com.BPO.plantcare.domain.repository.PlantTaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlantTasksUseCase @Inject constructor(
    private val repository: PlantTaskRepository,
) {
    operator fun invoke(plantId: Long): Flow<List<PlantTask>> =
        repository.observeForPlant(plantId)
}
