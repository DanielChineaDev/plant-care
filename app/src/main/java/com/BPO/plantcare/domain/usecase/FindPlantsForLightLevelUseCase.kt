package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.LightLevel
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import javax.inject.Inject

/**
 * Devuelve las plantas del catalogo cuyo requisito de luz se cumple con
 * el [level] medido (igual o mas exigente).
 *
 * Lux ascendente: LOW < MEDIUM < INDIRECT_BRIGHT < DIRECT < FULL_SUN.
 * Una planta que necesita MEDIUM esta feliz en MEDIUM, INDIRECT_BRIGHT,
 * DIRECT o FULL_SUN. Pero una de FULL_SUN no encaja en LOW.
 */
class FindPlantsForLightLevelUseCase @Inject constructor(
    private val catalog: PlantCatalogRepository,
) {
    operator fun invoke(level: LightLevel): List<PlantCareGuide> {
        val ordinal = level.ordinal
        return catalog.all()
            .filter { it.light.ordinal <= ordinal }
            .sortedBy { it.scientificName }
    }
}
