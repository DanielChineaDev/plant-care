package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.GlobalSearchResult

interface GlobalSearchRepository {

    /**
     * Busca un termino en comunidades y usuarios via Firestore con
     * prefix-match. NO incluye especies (eso lo resuelve el catalogo
     * local en el ViewModel). Devuelve mezcla de tipos.
     */
    suspend fun search(query: String, limitPerType: Int = 10): Result<List<GlobalSearchResult>>
}
