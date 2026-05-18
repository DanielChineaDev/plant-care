package com.BPO.plantcare.domain.repository

import com.BPO.plantcare.domain.model.PublicPlant
import com.BPO.plantcare.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface PublicProfileRepository {
    /** Observa el perfil publico del usuario [uid] en Firestore. */
    fun observeUserProfile(uid: String): Flow<UserProfile?>

    /** Plantas publicas del usuario [uid] (vacio si la coleccion es privada). */
    fun observePublicPlants(uid: String): Flow<List<PublicPlant>>

    /**
     * Cambia el flag isCollectionPublic del usuario actual.
     * - Si pasa a true: sube todas las plantas locales como public mirror.
     * - Si pasa a false: borra todo el mirror.
     */
    suspend fun setMyCollectionPublic(enabled: Boolean): Result<Unit>

    /** Resincroniza el mirror publico con las plantas locales actuales. */
    suspend fun resyncMyPublicCollection(): Result<Unit>
}
