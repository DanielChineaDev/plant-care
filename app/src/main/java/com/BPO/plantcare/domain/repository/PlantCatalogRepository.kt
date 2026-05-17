package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantCareGuide

interface PlantCatalogRepository {
    fun all(): List<PlantCareGuide>

    /** Match exacto por nombre cientifico (case-insensitive). */
    fun findByScientificName(scientificName: String): PlantCareGuide?

    /** Primera entrada del catalogo que pertenezca a ese genero. */
    fun findByGenus(genus: String): PlantCareGuide?
}
