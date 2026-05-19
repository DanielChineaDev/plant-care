package com.BPO.plantcare.domain.model

/**
 * Resultado unico de la busqueda global. Cada tipo agrupa una pantalla
 * de destino al tocarlo.
 */
sealed interface GlobalSearchResult {
    val id: String
    val displayName: String
    val subtitle: String?
    val imageUrl: String?

    data class Species(
        override val id: String,
        override val displayName: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        val scientificName: String,
    ) : GlobalSearchResult

    data class CommunityResult(
        override val id: String,
        override val displayName: String,
        override val subtitle: String?,
        override val imageUrl: String?,
        val emoji: String,
    ) : GlobalSearchResult

    data class UserResult(
        override val id: String, // uid
        override val displayName: String,
        override val subtitle: String?,
        override val imageUrl: String?,
    ) : GlobalSearchResult
}
