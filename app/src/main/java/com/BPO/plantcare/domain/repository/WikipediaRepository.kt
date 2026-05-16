package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.WikipediaSummary

interface WikipediaRepository {
    /**
     * Devuelve el resumen de Wikipedia para el nombre cientifico dado.
     * Intenta primero en espanol, despues en ingles. Devuelve null si no hay pagina.
     */
    suspend fun getSummary(scientificName: String): Result<WikipediaSummary?>
}
