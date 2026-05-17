package com.BPO.plantcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CareDifficulty(val label: String) {
    EASY("Fácil"),
    MEDIUM("Media"),
    HARD("Difícil"),
    EXPERT("Extrema"),
    PRO("Profesional"),
}

@Serializable
enum class LightLevel(val label: String) {
    LOW("Poca luz"),
    MEDIUM("Luz media"),
    INDIRECT_BRIGHT("Luz indirecta brillante"),
    DIRECT("Sol directo (algunas horas)"),
    FULL_SUN("Pleno sol"),
}

@Serializable
enum class HumidityLevel(val label: String) {
    LOW("Ambiente seco"),
    MEDIUM("Humedad media"),
    MEDIUM_HIGH("Humedad media-alta"),
    HIGH("Humedad alta"),
}

@Serializable
data class PlantCareGuide(
    val scientificName: String,
    val commonNames: List<String> = emptyList(),
    val difficulty: CareDifficulty,
    val indoor: Boolean,
    val outdoor: Boolean,
    val wateringIntervalDays: Int,
    val wateringNotes: String? = null,
    val light: LightLevel,
    val humidity: HumidityLevel,
    val substrate: String,
    val fertilizing: String,
    val repotting: String,
    val toxicToPets: Boolean,
    val funFact: String? = null,
)
