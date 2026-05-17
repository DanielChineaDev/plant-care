package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.WateringLog
import com.BPO.plantcare.domain.repository.WateringLogRepository
import javax.inject.Inject

/**
 * Inserta una entrada de log sin tocar lastWateredAt del plant. Util
 * para deshacer borrados desde la UI.
 */
class AddWateringLogUseCase @Inject constructor(
    private val repository: WateringLogRepository,
) {
    suspend operator fun invoke(log: WateringLog): Long = repository.add(log)
}
