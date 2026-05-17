package com.BPO.plantcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DiagnosisCategory(val label: String) {
    PEST("Plaga"),
    FUNGAL("Hongo"),
    BACTERIAL("Bacteria"),
    VIRAL("Virus"),
    PHYSIOLOGICAL("Problema fisiologico"),
}

@Serializable
enum class DiagnosisSeverity(val label: String) {
    LOW("Leve"),
    MEDIUM("Moderada"),
    HIGH("Grave"),
}

@Serializable
data class PlantDiagnosis(
    val id: String,
    val name: String,
    val scientificName: String? = null,
    val category: DiagnosisCategory,
    val severity: DiagnosisSeverity,
    val summary: String,
    val symptoms: List<String>,
    val causes: List<String>,
    val treatment: List<String>,
    val prevention: List<String>,
)
