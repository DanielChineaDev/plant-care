package com.BPO.plantcare.data.sync

import com.BPO.plantcare.domain.model.Plant
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Espejo en la nube de las plantas del usuario: users/{uid}/plants/{plantId}.
 * El id del documento es el id (Long) de Room, para que el espejo y la copia
 * local compartan identidad. Las fotos locales (userPhotoPath) se guardan tal
 * cual pero no se garantizan entre dispositivos; al no existir el fichero, la
 * UI cae al referenceImageUrl.
 */
@Singleton
class PlantCloudDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun plantsRef(uid: String) =
        firestore.collection(USERS).document(uid).collection(PLANTS)

    suspend fun upsert(uid: String, plant: Plant) {
        plantsRef(uid).document(plant.id.toString()).set(plant.toMap()).await()
    }

    suspend fun delete(uid: String, plantId: Long) {
        plantsRef(uid).document(plantId.toString()).delete().await()
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
        "addedAt" to addedAt,
        "lastWateredAt" to lastWateredAt,
        "wateringIntervalDays" to wateringIntervalDays.toLong(),
        "notes" to notes,
        "isOutdoor" to isOutdoor,
        "room" to room,
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
            addedAt = getLong("addedAt") ?: System.currentTimeMillis(),
            lastWateredAt = getLong("lastWateredAt"),
            wateringIntervalDays = (getLong("wateringIntervalDays") ?: 7L).toInt(),
            notes = getString("notes"),
            isOutdoor = getBoolean("isOutdoor"),
            room = getString("room"),
        )
    }

    companion object {
        private const val USERS = "users"
        private const val PLANTS = "plants"
    }
}
