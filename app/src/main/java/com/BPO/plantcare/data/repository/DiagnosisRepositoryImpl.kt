package com.BPO.plantcare.data.repository

import android.content.Context
import com.BPO.plantcare.domain.model.PlantDiagnosis
import com.BPO.plantcare.domain.repository.DiagnosisRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosisRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : DiagnosisRepository {

    private val diagnoses: List<PlantDiagnosis> by lazy { load() }
    private val byId: Map<String, PlantDiagnosis> by lazy { diagnoses.associateBy { it.id } }

    override fun all(): List<PlantDiagnosis> = diagnoses

    override fun findById(id: String): PlantDiagnosis? = byId[id]

    private fun load(): List<PlantDiagnosis> = runCatching {
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        json.decodeFromString<List<PlantDiagnosis>>(raw)
    }.getOrElse { emptyList() }

    companion object {
        private const val ASSET_FILE = "plant_diagnoses.json"
    }
}
