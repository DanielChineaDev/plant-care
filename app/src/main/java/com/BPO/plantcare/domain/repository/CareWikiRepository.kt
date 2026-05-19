package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.CareWikiContribution
import kotlinx.coroutines.flow.Flow

interface CareWikiRepository {

    /**
     * Contribuciones de la wiki para una especie. Se ordenan por createdAt
     * descendente.
     */
    fun observeContributions(scientificName: String): Flow<List<CareWikiContribution>>

    suspend fun addContribution(
        scientificName: String,
        wateringDays: Int?,
        fertilizeDays: Int?,
        lightLevel: String?,
        notes: String?,
    ): Result<String>

    suspend fun deleteContribution(scientificName: String, contributionId: String): Result<Unit>

    /**
     * Marca/desmarca una contribucion como aprobada. Solo los admins
     * deberian llamar esto; las reglas Firestore lo enforzan.
     */
    suspend fun setApproved(
        scientificName: String,
        contributionId: String,
        approved: Boolean,
    ): Result<Unit>
}
