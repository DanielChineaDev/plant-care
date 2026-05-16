package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantSuggestion
import java.io.File

interface PlantIdentificationRepository {
    /**
     * Envia una foto al servicio de identificacion y devuelve las posibles coincidencias
     * ordenadas por confianza (mayor primero).
     */
    suspend fun identify(image: File): Result<List<PlantSuggestion>>
}
