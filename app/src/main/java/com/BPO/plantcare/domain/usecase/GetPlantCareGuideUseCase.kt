package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.GuideMatch
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import javax.inject.Inject

class GetPlantCareGuideUseCase @Inject constructor(
    private val catalog: PlantCatalogRepository,
) {
    /**
     * Devuelve match exacto si la especie esta en el catalogo. Si no, intenta
     * por genero (primera palabra del nombre cientifico). Si nada de eso
     * coincide, devuelve null.
     */
    operator fun invoke(scientificName: String): GuideMatch? {
        catalog.findByScientificName(scientificName)?.let { return GuideMatch.Exact(it) }
        val genus = scientificName.trim().substringBefore(" ").takeIf { it.isNotBlank() }
            ?: return null
        return catalog.findByGenus(genus)?.let { GuideMatch.Genus(it, genus) }
    }
}
