package com.BPO.plantcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class DiagnosisCategory(@androidx.annotation.StringRes val labelRes: Int) {
    PEST(com.BPO.plantcare.R.string.diag_cat_pest),
    FUNGAL(com.BPO.plantcare.R.string.diag_cat_fungal),
    BACTERIAL(com.BPO.plantcare.R.string.diag_cat_bacterial),
    VIRAL(com.BPO.plantcare.R.string.diag_cat_viral),
    PHYSIOLOGICAL(com.BPO.plantcare.R.string.diag_cat_physiological),
}

@Serializable
enum class DiagnosisSeverity(@androidx.annotation.StringRes val labelRes: Int) {
    LOW(com.BPO.plantcare.R.string.diag_sev_low),
    MEDIUM(com.BPO.plantcare.R.string.diag_sev_medium),
    HIGH(com.BPO.plantcare.R.string.diag_sev_high),
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
