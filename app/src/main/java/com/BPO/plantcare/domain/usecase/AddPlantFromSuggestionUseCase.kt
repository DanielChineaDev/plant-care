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
    private val getCareGuide: GetPlantCareGuideUseCase,
) {
    suspend operator fun invoke(
        suggestion: PlantSuggestion,
        capturedPhoto: File?,
        nickname: String? = null,
        wateringIntervalDays: Int? = null,
    ): Result<Long> = runCatching {
        val persistedPath = capturedPhoto?.let { photoStorage.persist(it) }
        val match = getCareGuide(suggestion.scientificName)
        val guide = match?.guide
        val interval = wateringIntervalDays
            ?: guide?.wateringIntervalDays
            ?: DEFAULT_WATERING_DAYS
        // Inferimos exterior/interior del catalogo. Si la especie es ambigua
        // (indoor y outdoor a la vez) dejamos null para que el usuario decida.
        val inferredOutdoor: Boolean? = when {
            guide == null -> null
            guide.indoor && guide.outdoor -> null
            guide.outdoor -> true
            guide.indoor -> false
            else -> null
        }
        val plant = Plant(
            nickname = nickname,
            scientificName = suggestion.scientificName,
            commonName = suggestion.commonNames.firstOrNull() ?: guide?.commonNames?.firstOrNull(),
            family = suggestion.family,
            genus = suggestion.genus,
            referenceImageUrl = suggestion.imageUrl,
            userPhotoPath = persistedPath,
            addedAt = System.currentTimeMillis(),
            lastWateredAt = null,
            wateringIntervalDays = interval,
            notes = null,
            isOutdoor = inferredOutdoor,
        )
        repository.add(plant)
    }

    companion object {
        const val DEFAULT_WATERING_DAYS = 7
    }
}
