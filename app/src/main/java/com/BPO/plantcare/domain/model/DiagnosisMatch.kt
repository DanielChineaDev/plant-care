package com.BPO.plantcare.domain.model

/**
 * Resultado del diagnostico de una planta por foto.
 *
 * Hasta que tengamos una API real de diagnostico de plagas, el repo
 * stub devuelve matches sinteticos del catalogo local. La UI lo
 * indica visualmente con un disclaimer "modo demo".
 */
data class DiagnosisMatch(
    val diagnosis: PlantDiagnosis,
    val confidence: Float, // 0.0..1.0
)

data class DiagnosisAnalysis(
    val matches: List<DiagnosisMatch>,
    val isDemo: Boolean,
)
