package com.BPO.plantcare.data.sync

import android.net.Uri
import com.BPO.plantcare.domain.model.PlantPhoto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Espejo en la nube de las fotos de la galeria de cada planta:
 *  - Imagen en Storage: users/{uid}/plants/{plantId}/photos/{photoId}.jpg
 *  - Metadatos en Firestore: users/{uid}/plants/{plantId}/photos/{photoId}
 *
 * El id del documento es el id (Long) de Room, para que el espejo y la copia
 * local compartan identidad. Permite recuperar el diario fotografico al
 * cambiar de dispositivo.
 */
@Singleton
class PlantPhotoCloudDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private fun photosRef(uid: String, plantId: Long) =
        firestore.collection(USERS).document(uid)
            .collection(PLANTS).document(plantId.toString())
            .collection(PHOTOS)

    /**
     * Sube la imagen local a Storage, guarda los metadatos en Firestore y
     * devuelve la URL de descarga. Lanza si falla.
     */
    suspend fun upsert(uid: String, photo: PlantPhoto): String {
        val ref = storage.reference
            .child("$USERS/$uid/$PLANTS/${photo.plantId}/$PHOTOS/${photo.id}.jpg")
        val file = File(photo.path)
        ref.putFile(Uri.fromFile(file)).await()
        val url = ref.downloadUrl.await().toString()
        photosRef(uid, photo.plantId).document(photo.id.toString())
            .set(photo.toMap(url)).await()
        return url
    }

    suspend fun delete(uid: String, plantId: Long, photoId: Long) {
        runCatching {
            storage.reference
                .child("$USERS/$uid/$PLANTS/$plantId/$PHOTOS/$photoId.jpg")
                .delete().await()
        }
        runCatching {
            photosRef(uid, plantId).document(photoId.toString()).delete().await()
        }
    }

    suspend fun fetchForPlant(uid: String, plantId: Long): List<PlantPhoto> =
        photosRef(uid, plantId).get().await().documents
            .mapNotNull { it.toPhoto(plantId) }

    private fun PlantPhoto.toMap(url: String): Map<String, Any?> = mapOf(
        "id" to id,
        "plantId" to plantId,
        "timestamp" to timestamp,
        "note" to note,
        "remoteUrl" to url,
    )

    private fun DocumentSnapshot.toPhoto(plantId: Long): PlantPhoto? {
        if (!exists()) return null
        val photoId = getLong("id") ?: this.id.toLongOrNull() ?: return null
        val url = getString("remoteUrl") ?: return null
        return PlantPhoto(
            id = photoId,
            plantId = getLong("plantId") ?: plantId,
            // No hay ruta local en este dispositivo; la UI usa remoteUrl.
            path = "",
            timestamp = getLong("timestamp") ?: System.currentTimeMillis(),
            note = getString("note"),
            remoteUrl = url,
        )
    }

    companion object {
        private const val USERS = "users"
        private const val PLANTS = "plants"
        private const val PHOTOS = "photos"
    }
}
