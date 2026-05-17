package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.WikipediaSummary

interface WikipediaRepository {
    /**
     * Devuelve el resumen de Wikipedia para el nombre cientifico dado.
     * Intenta primero en espanol, despues en ingles. Devuelve null si no hay pagina.
     */
    suspend fun getSummary(scientificName: String): Result<WikipediaSummary?>

    /**
     * Devuelve solo la URL de la miniatura. Cacheada en memoria para que las
     * grids de la app puedan pedir lo mismo muchas veces sin re-fetch.
     * Devuelve null si Wikipedia no tiene foto o no hay pagina.
     */
    suspend fun getThumbnailUrl(scientificName: String): String?
}
