package com.BPO.plantcare.data.repository

import android.content.Context
import com.BPO.plantcare.domain.model.PlantCareGuide
import com.BPO.plantcare.domain.repository.PlantCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantCatalogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : PlantCatalogRepository {

    private val catalog: List<PlantCareGuide> by lazy { loadCatalog() }
    private val byName: Map<String, PlantCareGuide> by lazy {
        catalog.associateBy { it.scientificName.lowercase().trim() }
    }

    override fun all(): List<PlantCareGuide> = catalog

    override fun findByScientificName(scientificName: String): PlantCareGuide? {
        val key = scientificName.lowercase().trim()
        return byName[key]
    }

    private fun loadCatalog(): List<PlantCareGuide> = runCatching {
        val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        json.decodeFromString<List<PlantCareGuide>>(raw)
    }.getOrElse { emptyList() }

    companion object {
        private const val ASSET_FILE = "plant_catalog.json"
    }
}
