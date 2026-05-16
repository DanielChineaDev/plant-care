package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.repository.WateringLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWateringHistoryUseCase @Inject constructor(
    private val repository: WateringLogRepository,
) {
    operator fun invoke(plantId: Long): Flow<List<WateringLog>> =
        repository.observeForPlant(plantId)
}
