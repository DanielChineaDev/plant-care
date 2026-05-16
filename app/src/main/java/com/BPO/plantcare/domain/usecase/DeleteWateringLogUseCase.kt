package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.repository.WateringLogRepository
import javax.inject.Inject

class DeleteWateringLogUseCase @Inject constructor(
    private val repository: WateringLogRepository,
) {
    suspend operator fun invoke(logId: Long) {
        repository.delete(logId)
    }
}
