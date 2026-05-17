package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PlantDiagnosis

interface DiagnosisRepository {
    fun all(): List<PlantDiagnosis>
    fun findById(id: String): PlantDiagnosis?
}
