package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import javax.inject.Inject

class GetPlantCareGuideUseCase @Inject constructor(
    private val catalog: PlantCatalogRepository,
) {
    operator fun invoke(scientificName: String): PlantCareGuide? =
        catalog.findByScientificName(scientificName)
}
