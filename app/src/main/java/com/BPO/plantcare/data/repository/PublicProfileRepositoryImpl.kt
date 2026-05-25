package com.BPO.plantcare.data.repository

import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import com.BPO.plantcare.domain.repository.PlantRepository
import com.BPO.plantcare.domain.repository.PublicProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val plantRepository: PlantRepository,
) : PublicProfileRepository {

    private fun requireUid(): String =
        firebaseAuth.currentUser?.uid ?: error("Inicia sesion para hacer esta accion")

    override fun observeUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val reg = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null); return@addSnapshotListener
                }
                trySend(snap?.toUserProfile())
            }
        awaitClose { reg.remove() }
    }

    override fun observePublicPlants(uid: String): Flow<List<PublicPlant>> = callbackFlow {
        val reg = firestore.collection(USERS).document(uid)
            .collection(PUBLIC_PLANTS)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                trySend(snap?.documents?.mapNotNull { it.toPublicPlant() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    override suspend fun setMyCollectionPublic(enabled: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        // ORDEN IMPORTANTE en el caso "hacer privada":
        // primero borramos las plantas publicadas y SOLO si eso va bien
        // marcamos el flag a false. Si lo hicieramos al reves y fallase
        // el clearMirror, el flag quedaria en false pero las plantas
        // seguirian leyendose por las reglas (read: if true sobre publicPlants).
        if (!enabled) clearMirror(uid)
        firestore.collection(USERS).document(uid)
            .update("isCollectionPublic", enabled).await()
        if (enabled) syncCurrentPlants(uid)
    }

    override suspend fun resyncMyPublicCollection(): Result<Unit> = runCatching {
        val uid = requireUid()
        // Borramos y reescribimos para reflejar bajas/edits que no se hayan
        // propagado automaticamente.
        clearMirror(uid)
        syncCurrentPlants(uid)
    }

    private suspend fun syncCurrentPlants(uid: String) {
        val plants: List<Plant> = plantRepository.observeAll().first()
        if (plants.isEmpty()) return
        val batch = firestore.batch()
        val baseRef = firestore.collection(USERS).document(uid).collection(PUBLIC_PLANTS)
        plants.forEach { plant ->
            val doc = baseRef.document(plant.id.toString())
            val data = mapOf(
                "scientificName" to plant.scientificName,
                "commonName" to plant.commonName,
                "nickname" to plant.nickname,
                "referenceImageUrl" to plant.referenceImageUrl,
                "userPhotoUrl" to plant.userPhotoUrl,
                "addedAt" to plant.addedAt,
                "photosPublic" to plant.photosPublic,
                "notesPublic" to plant.notesPublic,
                "syncedAt" to FieldValue.serverTimestamp(),
            )
            batch.set(doc, data)
        }
        batch.commit().await()
    }

    private suspend fun clearMirror(uid: String) {
        val baseRef = firestore.collection(USERS).document(uid).collection(PUBLIC_PLANTS)
        val docs = baseRef.get().await().documents
        if (docs.isEmpty()) return
        // Firestore acepta hasta 500 deletes por batch. Con coleccion personal
        // suficiente para v1.
        val batch = firestore.batch()
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    override fun observeAchievements(uid: String): Flow<Map<String, Long>> = callbackFlow {
        val reg = firestore.collection(USERS).document(uid)
            .collection(ACHIEVEMENTS)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyMap()); return@addSnapshotListener
                }
                val map = snap?.documents?.associate { doc ->
                    doc.id to ((doc.getDate("unlockedAt") ?: Date(0)).time)
                }.orEmpty()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun recordAchievement(achievementId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val ref = firestore.collection(USERS).document(uid)
            .collection(ACHIEVEMENTS).document(achievementId)
        // Solo escribimos si no existe, para no pisar la fecha original.
        val existing = ref.get().await()
        if (!existing.exists()) {
            ref.set(mapOf("unlockedAt" to FieldValue.serverTimestamp())).await()
        }
    }

    override suspend fun setBadgesPublic(enabled: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        firestore.collection(USERS).document(uid)
            .update("badgesPublic", enabled).await()
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            uid = id,
            displayName = getString("displayName"),
            email = getString("email"),
            photoUrl = getString("photoUrl"),
            createdAt = getLong("createdAt") ?: 0L,
            isCollectionPublic = getBoolean("isCollectionPublic") ?: false,
            isAdmin = getBoolean("isAdmin") ?: false,
            karma = getLong("karma") ?: 0L,
            bio = getString("bio"),
            location = getString("location"),
            favoritePlants = (get("favoritePlants") as? List<*>)
                ?.filterIsInstance<String>().orEmpty(),
            badgesPublic = getBoolean("badgesPublic") ?: true,
        )
    }

    private fun DocumentSnapshot.toPublicPlant(): PublicPlant? {
        if (!exists()) return null
        return PublicPlant(
            id = id,
            scientificName = getString("scientificName").orEmpty(),
            commonName = getString("commonName"),
            nickname = getString("nickname"),
            referenceImageUrl = getString("referenceImageUrl"),
            userPhotoUrl = getString("userPhotoUrl"),
            addedAt = getLong("addedAt") ?: (getDate("addedAt") ?: Date(0)).time,
            photosPublic = getBoolean("photosPublic") ?: false,
            notesPublic = getBoolean("notesPublic") ?: false,
        )
    }

    companion object {
        private const val USERS = "users"
        private const val PUBLIC_PLANTS = "publicPlants"
        private const val ACHIEVEMENTS = "achievements"
    }
}
