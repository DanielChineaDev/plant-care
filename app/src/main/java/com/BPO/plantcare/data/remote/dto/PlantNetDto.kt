package com.BPO.plantcare.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlantNetResponseDto(
    val results: List<PlantNetResultDto> = emptyList(),
)

@Serializable
data class PlantNetResultDto(
    val score: Double = 0.0,
    val species: PlantNetSpeciesDto,
    val images: List<PlantNetImageDto> = emptyList(),
    val gbif: PlantNetGbifDto? = null,
)

@Serializable
data class PlantNetSpeciesDto(
    val scientificNameWithoutAuthor: String = "",
    val scientificNameAuthorship: String = "",
    val genus: PlantNetTaxonDto? = null,
    val family: PlantNetTaxonDto? = null,
    val commonNames: List<String> = emptyList(),
    val scientificName: String = "",
)

@Serializable
data class PlantNetTaxonDto(
    val scientificNameWithoutAuthor: String = "",
)

@Serializable
data class PlantNetImageDto(
    val url: PlantNetImageUrlDto? = null,
    val citation: String? = null,
    @SerialName("organ") val organ: String? = null,
)

@Serializable
data class PlantNetImageUrlDto(
    val s: String? = null,
    val m: String? = null,
    val o: String? = null,
)

@Serializable
data class PlantNetGbifDto(
    val id: String = "",
)
