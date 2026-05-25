package com.BPO.plantcare.data.sync

import android.net.Uri
import com.BPO.plantcare.domain.model.Plant
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Espejo en la nube de las plantas del usuario: users/{uid}/plants/{plantId}.
 * El id del documento es el id (Long) de Room, para que el espejo y la copia
 * local compartan identidad. La foto principal del usuario se sube a Storage
 * (users/{uid}/plants/{plantId}/main.jpg) y su URL se guarda en `userPhotoUrl`,
 * de modo que sobrevive al cambio de dispositivo.
 */
@Singleton
class PlantCloudDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private fun plantsRef(uid: String) =
        firestore.collection(USERS).document(uid).collection(PLANTS)

    suspend fun upsert(uid: String, plant: Plant) {
        plantsRef(uid).document(plant.id.toString()).set(plant.toMap()).await()
    }

    /**
     * Sube la foto principal local a Storage y devuelve su URL de descarga.
     * Lanza si falla (el caller decide si ignora el error).
     */
    suspend fun uploadMainPhoto(uid: String, plantId: Long, file: File): String {
        val ref = storage.reference.child("$USERS/$uid/$PLANTS/$plantId/main.jpg")
        ref.putFile(Uri.fromFile(file)).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun delete(uid: String, plantId: Long) {
        plantsRef(uid).document(plantId.toString()).delete().await()
        // Borra cualquier foto de esta planta en Storage (best-effort).
        runCatching {
            storage.reference.child("$USERS/$uid/$PLANTS/$plantId").listAll().await()
                .let { result ->
                    result.items.forEach { runCatching { it.delete().await() } }
                    result.prefixes.forEach { prefix ->
                        runCatching {
                            prefix.listAll().await().items.forEach { it.delete().await() }
                        }
                    }
                }
        }
    }

    suspend fun fetchAll(uid: String): List<Plant> =
        plantsRef(uid).get().await().documents.mapNotNull { it.toPlant() }

    private fun Plant.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "nickname" to nickname,
        "scientificName" to scientificName,
        "commonName" to commonName,
        "family" to family,
        "genus" to genus,
        "referenceImageUrl" to referenceImageUrl,
        "userPhotoPath" to userPhotoPath,
        "userPhotoUrl" to userPhotoUrl,
        "addedAt" to addedAt,
        "lastWateredAt" to lastWateredAt,
        "wateringIntervalDays" to wateringIntervalDays.toLong(),
        "notes" to notes,
        "isOutdoor" to isOutdoor,
        "room" to room,
        "photosPublic" to photosPublic,
        "notesPublic" to notesPublic,
    )

    private fun DocumentSnapshot.toPlant(): Plant? {
        if (!exists()) return null
        val plantId = getLong("id") ?: this.id.toLongOrNull() ?: return null
        return Plant(
            id = plantId,
            nickname = getString("nickname"),
            scientificName = getString("scientificName").orEmpty(),
            commonName = getString("commonName"),
            family = getString("family"),
            genus = getString("genus"),
            referenceImageUrl = getString("referenceImageUrl"),
            userPhotoPath = getString("userPhotoPath"),
            userPhotoUrl = getString("userPhotoUrl"),
            addedAt = getLong("addedAt") ?: System.currentTimeMillis(),
            lastWateredAt = getLong("lastWateredAt"),
            wateringIntervalDays = (getLong("wateringIntervalDays") ?: 7L).toInt(),
            notes = getString("notes"),
            isOutdoor = getBoolean("isOutdoor"),
            room = getString("room"),
            photosPublic = getBoolean("photosPublic") ?: false,
            notesPublic = getBoolean("notesPublic") ?: false,
        )
    }

    companion object {
        private const val USERS = "users"
        private const val PLANTS = "plants"
    }
}
