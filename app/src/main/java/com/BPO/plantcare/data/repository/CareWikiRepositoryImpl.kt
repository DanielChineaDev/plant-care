package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.CareWikiContribution
import com.BPO.plantcare.domain.repository.CareWikiRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CareWikiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : CareWikiRepository {

    override fun observeContributions(scientificName: String): Flow<List<CareWikiContribution>> =
        callbackFlow {
            val key = scientificName.normalizeKey()
            val reg = firestore.collection(CARE_WIKI).document(key)
                .collection(CONTRIBUTIONS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(emptyList()); return@addSnapshotListener
                    }
                    val list = snap?.documents
                        ?.mapNotNull { it.toContribution(scientificName) }
                        .orEmpty()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }

    override suspend fun addContribution(
        scientificName: String,
        wateringDays: Int?,
        fertilizeDays: Int?,
        lightLevel: String?,
        notes: String?,
    ): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: error("Inicia sesion para contribuir")
        require(
            wateringDays != null || fertilizeDays != null ||
                lightLevel != null || !notes.isNullOrBlank(),
        ) { "Anade al menos un dato (riego, abono, luz o nota)" }

        val key = scientificName.normalizeKey()
        val doc = firestore.collection(CARE_WIKI).document(key)
            .collection(CONTRIBUTIONS).document()
        val data = mapOf(
            "authorUid" to user.uid,
            "authorName" to (user.displayName ?: ""),
            "authorPhoto" to user.photoUrl?.toString(),
            "scientificName" to scientificName,
            "wateringDays" to wateringDays,
            "fertilizeDays" to fertilizeDays,
            "lightLevel" to lightLevel,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        doc.set(data).await()
        doc.id
    }

    override suspend fun deleteContribution(
        scientificName: String,
        contributionId: String,
    ): Result<Unit> = runCatching {
        val key = scientificName.normalizeKey()
        firestore.collection(CARE_WIKI).document(key)
            .collection(CONTRIBUTIONS).document(contributionId)
            .delete().await()
    }

    override suspend fun setApproved(
        scientificName: String,
        contributionId: String,
        approved: Boolean,
    ): Result<Unit> = runCatching {
        val key = scientificName.normalizeKey()
        firestore.collection(CARE_WIKI).document(key)
            .collection(CONTRIBUTIONS).document(contributionId)
            .update("approved", approved).await()
    }

    private fun DocumentSnapshot.toContribution(originalName: String): CareWikiContribution? {
        if (!exists()) return null
        return CareWikiContribution(
            id = id,
            scientificName = getString("scientificName") ?: originalName,
            authorUid = getString("authorUid").orEmpty(),
            authorName = getString("authorName"),
            authorPhoto = getString("authorPhoto"),
            wateringDays = getLong("wateringDays")?.toInt(),
            fertilizeDays = getLong("fertilizeDays")?.toInt(),
            lightLevel = getString("lightLevel"),
            notes = getString("notes"),
            createdAt = (getDate("createdAt") ?: Date(0)).time,
            approved = getBoolean("approved") ?: false,
        )
    }

    /**
     * Normaliza el nombre cientifico para usarlo como id de documento.
     * Firestore no acepta "/" en el id, y queremos colapsar mayusculas/
     * espacios.
     */
    private fun String.normalizeKey(): String = trim().lowercase()
        .replace(Regex("\\s+"), "_")
        .replace("/", "_")

    companion object {
        private const val CARE_WIKI = "careWiki"
        private const val CONTRIBUTIONS = "contributions"
    }
}
