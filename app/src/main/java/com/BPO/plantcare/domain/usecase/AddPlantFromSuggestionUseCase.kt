package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.core.storage.PhotoStorage
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PlantSuggestion
import com.BPO.plantcare.domain.repository.PlantRepository
import java.io.File
import javax.inject.Inject

class AddPlantFromSuggestionUseCase @Inject constructor(
    private val repository: PlantRepository,
    private val photoStorage: PhotoStorage,
) {
    suspend operator fun invoke(
        suggestion: PlantSuggestion,
        capturedPhoto: File?,
        nickname: String? = null,
        wateringIntervalDays: Int = DEFAULT_WATERING_DAYS,
    ): Result<Long> = runCatching {
        val persistedPath = capturedPhoto?.let { photoStorage.persist(it) }
        val plant = Plant(
            nickname = nickname,
            scientificName = suggestion.scientificName,
            commonName = suggestion.commonNames.firstOrNull(),
            family = suggestion.family,
            genus = suggestion.genus,
            referenceImageUrl = suggestion.imageUrl,
            userPhotoPath = persistedPath,
            addedAt = System.currentTimeMillis(),
            lastWateredAt = null,
            wateringIntervalDays = wateringIntervalDays,
            notes = null,
        )
        repository.add(plant)
    }

    companion object {
        const val DEFAULT_WATERING_DAYS = 7
    }
}
