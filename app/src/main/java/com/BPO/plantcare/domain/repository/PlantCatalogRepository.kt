package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantCareGuide

interface PlantCatalogRepository {
    fun all(): List<PlantCareGuide>

    /**
     * Busca por nombre cientifico exacto (case-insensitive).
     * Devuelve null si no esta en el catalogo.
     */
    fun findByScientificName(scientificName: String): PlantCareGuide?
}
