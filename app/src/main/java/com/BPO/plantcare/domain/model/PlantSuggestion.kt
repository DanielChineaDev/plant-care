package com.BPO.plantcare.domain.model

/**
 * Una posible coincidencia devuelta por el servicio de identificacion (PlantNet, etc.).
 *
 * @param score Confianza entre 0.0 y 1.0.
 */
data class PlantSuggestion(
    val scientificName: String,
    val commonNames: List<String>,
    val family: String?,
    val genus: String?,
    val score: Double,
    val imageUrl: String?,
)
